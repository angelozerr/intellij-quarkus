package com.redhat.devtools.intellij.lsp4ij.operations.symbols;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class LSPWorkspaceSymbolsContributor implements ChooseByNameContributorEx {

    private final static Logger LOGGER = LoggerFactory.getLogger(LSPWorkspaceSymbolsContributor.class);

    @Override
    public void processNames(@NotNull Processor<? super String> processor,
                             @NotNull GlobalSearchScope scope,
                             @Nullable IdFilter filter) {
        Project project = Objects.requireNonNull(scope.getProject());
        List<String> propertyKeys = Arrays.asList("@");
        ContainerUtil.process(propertyKeys, processor);
    }

    @Override
    public void processElementsWithName(@NotNull String name,
                                        @NotNull Processor<? super NavigationItem> processor,
                                        @NotNull FindSymbolParameters parameters) {

        WorkspaceSymbolParams params = new WorkspaceSymbolParams();
        LanguageServiceAccessor.getInstance(parameters.getProject())
                .getStartedServers()
                .forEach(ls -> {
                    if (canSupportWorkspaceSymbol(ls.getServerCapabilities())) {

                        var result = ls.getServer().getWorkspaceService().symbol(params);
                        result.join();
                        result.thenApply(symbols -> {
                            if (symbols.isLeft()) {
                                List<? extends SymbolInformation> s = symbols.getLeft();
                                for (var si : s) {
                                    ContainerUtil.process(Arrays.asList(new LSPNavigationItem(
                                            si.getName()
                                    )), processor);
                                }
                            } else if (symbols.isRight()) {
                                List<? extends WorkspaceSymbol> ws = symbols.getRight();
                                for (var si : ws) {
                                    ContainerUtil.process(Arrays.asList(new LSPNavigationItem(
                                            si.getName()
                                    )), processor);
                                }
                            }
                            return null;
                        }).exceptionally(e -> {
                            LOGGER.error("Error while workspace symbols with " + ls.serverDefinition.id, e);
                            return null;
                        });
                    }
                });
//    List<NavigationItem> properties = Arrays.asList(new LSPNavigationItem("CCC"));
            /*ContainerUtil.map(
            SimpleUtil.findProperties(parameters.getProject(), name),
            property -> (NavigationItem) property);*/
        //  ContainerUtil.process(properties, processor);
    }

    private static boolean canSupportWorkspaceSymbol(ServerCapabilities serverCapabilities) {
        if (serverCapabilities == null || serverCapabilities.getWorkspaceSymbolProvider() == null) {
            return false;
        }
        var provider = serverCapabilities.getWorkspaceSymbolProvider();
        if (provider.isLeft()) {
            return provider.getLeft() != null ? provider.getLeft() : false;
        }
        if (provider.isRight()) {
            return provider.getRight() != null;
        }
        return false;
    }

}