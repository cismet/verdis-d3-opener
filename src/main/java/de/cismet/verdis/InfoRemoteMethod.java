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
@Path("/info/")
@ServiceProvider(service = RESTRemoteControlMethod.class)
public class InfoRemoteMethod extends AbstractRESTRemoteControlMethod {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(InfoRemoteMethod.class);

    //~ Instance fields --------------------------------------------------------

    @Context private UriInfo context;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GoToKassenzeichenRemoteMethod object.
     */
    public InfoRemoteMethod() {
        super(-1, "/info/");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @GET
    @Produces("text/html")
    public Response available() {
        return Response.status(Status.OK).entity(getInfo()).build();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getInfo() {
        return "\\o/";
    }
}
