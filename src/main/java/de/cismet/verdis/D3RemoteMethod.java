/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.verdis;

import org.openide.util.lookup.ServiceProvider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import de.cismet.remote.AbstractRESTRemoteControlMethod;
import de.cismet.remote.RESTRemoteControlMethod;
/**
 * DOCUMENT ME!
 *
 * @author   spuhl
 * @author   Benjamin Friedrich (benjamin.friedrich@cismet.de)
 * @version  $Revision$, $Date$
 */
@Path("/d3/")
@ServiceProvider(service = RESTRemoteControlMethod.class)
public class D3RemoteMethod extends AbstractRESTRemoteControlMethod {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(D3RemoteMethod.class);

    //~ Instance fields --------------------------------------------------------

    @Context private UriInfo context;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GoToKassenzeichenRemoteMethod object.
     */
    public D3RemoteMethod() {
        super(-1, "/d3/");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   kassenzeichen  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @GET
    @Produces("application/json")
    public Response gotoKassenzeichen(@QueryParam("kassenzeichen") final String kassenzeichen) {
        if (log.isDebugEnabled()) {
            log.debug("D3 Opener: gotoKassenzeichen");
        }

        try {
            final String host = context.getBaseUri().getHost();
            if (!host.equals("localhost") && !host.equals("127.0.0.1")) {
                log.info("Keine Request von remote rechnern möglich: " + host);
                return Response.status(Status.SERVICE_UNAVAILABLE)
                            .entity("not possible from remote")
                            .header("Access-Control-Allow-Origin", "*")
                            .build();
            } else {
                try {
                    final Runtime rt = Runtime.getRuntime();
                    final Process pr = rt.exec("clink.exe verdis " + kassenzeichen);
                    return Response.status(Status.OK)
                                .entity("Success \\o/")
                                .header("Access-Control-Allow-Origin", "*")
                                .build();
                } catch (Exception e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity(e.getMessage())
                                .header("Access-Control-Allow-Origin", "*")
                                .build();
                }
            }
        } catch (Exception ex) {
            log.error("Fehler beim bestimmen des Hosts Request nicht möglich");
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Could not determine casller host:" + ex.getMessage())
                        .header("Access-Control-Allow-Origin", "*")
                        .build();
        }
    }
}
