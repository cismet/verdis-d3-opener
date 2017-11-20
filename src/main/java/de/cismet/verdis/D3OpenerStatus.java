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

import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import de.cismet.remote.AbstractRESTRemoteControlMethod;
import de.cismet.remote.RESTRemoteControlMethod;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@Path("/open-d3-available/")
@ServiceProvider(service = RESTRemoteControlMethod.class)
public class D3OpenerStatus extends AbstractRESTRemoteControlMethod {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(D3OpenerStatus.class);

    //~ Instance fields --------------------------------------------------------

    @Context private UriInfo context;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GoToKassenzeichenRemoteMethod object.
     */
    public D3OpenerStatus() {
        super(-1, "/open-d3-available/");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @HEAD
    @Produces("application/json")
    public Response available() {
        try {
            final String host = context.getBaseUri().getHost();
            if (!host.equals("localhost") && !host.equals("127.0.0.1")) {
                log.info("Keine Request von remote rechnern möglich: " + host);
                return Response.status(Status.SERVICE_UNAVAILABLE)
                            .entity("not possible from remote")
                            .header("Access-Control-Allow-Origin", "*")
                            .build();
            } else {
                return Response.status(Status.OK).header("Access-Control-Allow-Origin", "*").build();
            }
        } catch (Exception ex) {
            log.error("Fehler beim bestimmen des Hosts Request nicht möglich");
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Could not determine caller host:" + ex.getMessage())
                        .header("Access-Control-Allow-Origin", "*")
                        .build();
        }
    }
}
