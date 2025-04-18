package main

import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Path("/some-page")
class SomePage {

    @CheckedTemplate
    object Templates {
        @JvmStatic external fun hello(): TemplateInstance
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun hello(@QueryParam("count") count: Int?): TemplateInstance {
        return Templates.hello()
    }
}