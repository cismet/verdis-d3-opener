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

import Sirius.navigator.connection.ConnectionFactory;
import Sirius.navigator.connection.ConnectionInfo;
import Sirius.navigator.connection.ConnectionSession;
import Sirius.navigator.connection.SessionManager;
import Sirius.navigator.connection.proxy.ConnectionProxy;
import Sirius.navigator.event.CatalogueActivationListener;
import Sirius.navigator.event.CatalogueSelectionListener;
import Sirius.navigator.exception.ConnectionException;
import Sirius.navigator.resource.PropertyManager;
import Sirius.navigator.ui.ComponentRegistry;
import Sirius.navigator.ui.DescriptionPane;
import Sirius.navigator.ui.DescriptionPaneCalpa;
import Sirius.navigator.ui.DescriptionPaneFS;
import Sirius.navigator.ui.DescriptionPaneFX;
import Sirius.navigator.ui.LayoutedContainer;
import Sirius.navigator.ui.MutableMenuBar;
import Sirius.navigator.ui.MutableToolBar;
import Sirius.navigator.ui.attributes.AttributeViewer;
import Sirius.navigator.ui.attributes.editor.AttributeEditor;
import Sirius.navigator.ui.tree.MetaCatalogueTree;
import Sirius.navigator.ui.tree.PostfilterEnabledSearchResultsTree;
import Sirius.navigator.ui.tree.SearchResultsTree;
import Sirius.navigator.ui.tree.WorkingSpaceTree;

import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.middleware.types.Node;

import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.swing.ToolTipManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import de.cismet.cids.client.tools.ObjectRendererDialog;

import de.cismet.cids.navigator.utils.ClassCacheMultiple;

import de.cismet.cismap.commons.gui.MappingComponent;
import de.cismet.cismap.commons.interaction.CismapBroker;

import de.cismet.connectioncontext.AbstractConnectionContext;
import de.cismet.connectioncontext.ConnectionContext;

import de.cismet.lookupoptions.gui.OptionsClient;

import de.cismet.netutil.Proxy;
import de.cismet.netutil.ProxyHandler;

import de.cismet.remote.AbstractRESTRemoteControlMethod;
import de.cismet.remote.RESTRemoteControlMethod;

import de.cismet.security.WebAccessManager;

import de.cismet.tools.JnlpSystemPropertyHelper;

import de.cismet.tools.configuration.ConfigurationManager;
import de.cismet.tools.configuration.TakeoffHook;

import static Sirius.navigator.Navigator.NAVIGATOR_HOME;
import static Sirius.navigator.Navigator.NAVIGATOR_HOME_DIR;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@Path("/renderer/")
@ServiceProvider(service = RESTRemoteControlMethod.class)
public class RendererRemoteMethod extends AbstractRESTRemoteControlMethod {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(RendererRemoteMethod.class);
    private static volatile ObjectRendererDialog dialog = null;
    private static volatile boolean initialized = false;
    private static String log4JUrl = null;
    private static boolean l4jinited = false;

    //~ Instance fields --------------------------------------------------------

    @Context private UriInfo context;
    private final ConnectionContext CC = ConnectionContext.create(
            AbstractConnectionContext.Category.RENDERER,
            RendererRemoteMethod.class.getName());
    private final PropertyManager propertyManager;
    private final ConfigurationManager configurationManager = new ConfigurationManager();
    private final Preferences preferences;
    private LayoutedContainer container;
    private MutableMenuBar menuBar;
    private MutableToolBar toolBar;
    private MetaCatalogueTree metaCatalogueTree;
    private SearchResultsTree searchResultsTree;
    private WorkingSpaceTree workingSpaceTree;
    private AttributeViewer attributeViewer;
    private AttributeEditor attributeEditor;
    private DescriptionPane descriptionPane;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GoToKassenzeichenRemoteMethod object.
     */
    public RendererRemoteMethod() {
        super(-1, "/renderer/");
        this.propertyManager = PropertyManager.getManager();

        this.preferences = Preferences.userNodeForPackage(this.getClass());
        try {
            synchronized (LOG) {
                if (!initialized) {
                    init();
                    initialized = true;
                }
            }
        } catch (Exception e) {
            LOG.error("Cannot initialise renderer components", e);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  log4j          DOCUMENT ME!
     * @param  navigatorConf  DOCUMENT ME!
     * @param  cidsExt        DOCUMENT ME!
     * @param  pluginsUrl     DOCUMENT ME!
     * @param  searchUrl      DOCUMENT ME!
     * @param  profilesUrl    DOCUMENT ME!
     */
    public static void initStaticVariable(final String log4j,
            final String navigatorConf,
            final String cidsExt,
            final String pluginsUrl,
            final String searchUrl,
            final String profilesUrl) {
        RendererRemoteMethod.log4JUrl = log4j;

        try {
            PropertyManager.getManager().configure(navigatorConf, cidsExt, pluginsUrl, null, profilesUrl);
        } catch (Exception e) {
            LOG.error("Error while initialising the PropertyManager", e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   domain  DOCUMENT ME!
     * @param   jwt     DOCUMENT ME!
     * @param   table   DOCUMENT ME!
     * @param   id      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @GET
    @Produces("application/json")
    public Response openRenderer(@QueryParam("domain") final String domain,
            @QueryParam("jwt") final String jwt,
            @QueryParam("table") final String table,
            @QueryParam("id") final String id) {
        try {
            final String host = context.getBaseUri().getHost();
            if (!host.equals("localhost")
                        && !host.equals("127.0.0.1")) {
                LOG.info("Keine Request von remote rechnern möglich: " + host);
                return Response.status(Status.SERVICE_UNAVAILABLE)
                            .entity("not possible from remote")
                            .header("Access-Control-Allow-Origin", "*")
                            .build();
            } else {
                final String callserver = propertyManager.getConnectionInfo().getCallserverURL();
                String loginDomain = domain;

                loginDomain = getDomainFromJwt(jwt, loginDomain);

                initSessionManagerFromRestfulConnection(
                    callserver,
                    loginDomain,
                    jwt,
                    propertyManager.isCompressionEnabled());
                final List<Integer> ids = new ArrayList<>();
                final StringTokenizer st = new StringTokenizer(id, ",");

                while (st.hasMoreTokens()) {
                    final String idString = st.nextToken();

                    try {
                        ids.add(Integer.parseInt(idString));
                    } catch (NumberFormatException e) {
                        LOG.error("The string " + idString + " is not a valid id", e);
                    }
                }

                final MetaClass mc = ClassCacheMultiple.getMetaClass(domain, table, CC);
                final List<Node> nodes = new ArrayList<>();

                for (final Integer extractedId : ids) {
                    final MetaObject mo = SessionManager.getConnection()
                                .getMetaObject(SessionManager.getSession().getUser(),
                                    extractedId,
                                    mc.getId(),
                                    domain,
                                    CC);
                    nodes.add(new MetaObjectNode(mo.getBean()));
                }

                // the session must exist to initialise the mappingComponent
                initMappingComponent();

                if ((dialog == null) || !dialog.isVisible()) {
                    dialog = new ObjectRendererDialog(ComponentRegistry.getRegistry().getDescriptionPane());
                    dialog.setNodes(nodes.toArray(new Node[nodes.size()]));

                    dialog.setSize(1300, 800);
                    dialog.setVisible(true);
                } else {
                    dialog.setNodes(nodes.toArray(new Node[nodes.size()]));
                }

                return Response.status(Status.OK).header("Access-Control-Allow-Origin", "*").build();
            }
        } catch (Exception ex) {
            LOG.error("Fehler beim bestimmen des Hosts Request nicht möglich");
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\": \"Internal server error: " + ex.getMessage() + "\"}")
                        .header("Access-Control-Allow-Origin", "*")
                        .build();
        }
    }

    /**
     * try to extract the login domain from the payload.
     *
     * @param   jwt           DOCUMENT ME!
     * @param   defaultValue  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getDomainFromJwt(final String jwt, final String defaultValue) {
        try {
            final String payload = new String(Base64.getDecoder().decode(
                        jwt.substring(jwt.indexOf(".") + 1, jwt.lastIndexOf("."))),
                    "UTF-8");
            String loginDomain = payload.substring(payload.indexOf("\"domain\":\"") + "\"domain\":\"".length());
            loginDomain = loginDomain.substring(0, loginDomain.indexOf("\""));

            return loginDomain;
        } catch (Exception e) {
            LOG.error("Cannot extract the domain from the jwt", e);
        }

        return defaultValue;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   callserverUrl       DOCUMENT ME!
     * @param   domain              DOCUMENT ME!
     * @param   jwt                 DOCUMENT ME!
     * @param   compressionEnabled  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void initSessionManagerFromRestfulConnection(final String callserverUrl,
            final String domain,
            final String jwt,
            final boolean compressionEnabled) throws Exception {
        final ConnectionInfo info = new ConnectionInfo();
        info.setCallserverURL(callserverUrl);
        info.setUsername("jwt");
        info.setPassword(jwt);
        info.setUserDomain(domain);
        info.setUsergroupDomain(domain);

        // "Sirius.navigator.connection.RESTfulConnection"
        final Sirius.navigator.connection.Connection connection = ConnectionFactory.getFactory()
                    .createConnection(propertyManager.getConnectionClass(),
                        info.getCallserverURL(),
                        RendererRemoteMethod.class.getSimpleName(),
                        ProxyHandler.getInstance().getProxy(),
                        compressionEnabled,
                        CC);
        // "Sirius.navigator.connection.proxy.DefaultConnectionProxyHandler"
        final ConnectionSession session = ConnectionFactory.getFactory().createSession(connection, info, true, CC);
        final ConnectionProxy conProxy = ConnectionFactory.getFactory()
                    .createProxy(
                        propertyManager.getConnectionProxyClass(),
                        session,
                        CC);
        SessionManager.init(conProxy);

        ClassCacheMultiple.setInstance(domain, CC);
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void init() throws Exception {
        initTakeoffHooks();

        final String heavyComps = System.getProperty("contains.heavyweight.comps"); // NOI18N
        if ((heavyComps != null) && heavyComps.equals("true")) {                    // NOI18N
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        }

        try {
            checkNavigatorHome();
            loadLog4JPropertiesFromUrl();

            initProxy();
            initConfigurationManager();
            initUI();
            initWidgets();
            initDialogs();
            initEvents();

            configurationManager.addConfigurable(OptionsClient.getInstance());
            configurationManager.configure();
        } catch (final InterruptedException iexp) {
            LOG.error("navigator start interrupted: " + iexp.getMessage() + "\n disconnecting from server"); // NOI18N
            SessionManager.getSession().logout();
            SessionManager.getConnection().disconnect();
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void initMappingComponent() {
        final ConfigurationManager cismapConfigManager = new ConfigurationManager();
        String cismapconfig = null;
        String fallBackConfig = null;
        String dirExtension = null;
        try {
            final String ext = JnlpSystemPropertyHelper.getProperty("directory.extension"); // NOI18N

            System.out.println("SystemdirExtension=:" + ext); // NOI18N

            if (ext != null) {
                dirExtension = ext;
            }
        } catch (final Exception e) {
            LOG.warn("Error while adding DirectoryExtension"); // NOI18N
        }

        if (cismapconfig == null) {
            cismapconfig = "defaultCismapProperties.xml"; // NOI18N
        }

        if (fallBackConfig == null) {
            fallBackConfig = "defaultCismapProperties.xml"; // NOI18N
        }

        LOG.info("ServerConfigFile=" + cismapconfig);               // NOI18N
        cismapConfigManager.setDefaultFileName(cismapconfig);
        cismapConfigManager.setFallBackFileName(fallBackConfig);
        cismapConfigManager.setFileName("configurationPlugin.xml"); // NOI18N

        cismapConfigManager.setClassPathFolder("/");             // NOI18N
        cismapConfigManager.setFolder(".cismap" + dirExtension); // NOI18N

        final MappingComponent mapC = new MappingComponent(true);
        CismapBroker.getInstance().addCrsChangeListener(mapC);
        CismapBroker.getInstance().setMappingComponent(mapC);
        cismapConfigManager.addConfigurable(mapC);
        cismapConfigManager.configure();
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  InterruptedException  DOCUMENT ME!
     */
    private void initEvents() throws InterruptedException {
        final CatalogueSelectionListener catalogueSelectionListener = new CatalogueSelectionListener(
                attributeViewer,
                descriptionPane);
        metaCatalogueTree.addTreeSelectionListener(catalogueSelectionListener);
        searchResultsTree.addTreeSelectionListener(catalogueSelectionListener);

        metaCatalogueTree.addComponentListener(new CatalogueActivationListener(
                metaCatalogueTree,
                attributeViewer,
                descriptionPane));
        searchResultsTree.addComponentListener(new CatalogueActivationListener(
                searchResultsTree,
                attributeViewer,
                descriptionPane));
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void initWidgets() throws Exception {
        // MetaCatalogueTree ---------------------------------------------------
        metaCatalogueTree = new MetaCatalogueTree();

        // SearchResultsTree ---------------------------------------------------
        if (PropertyManager.getManager().isPostfilterEnabled()) {
            searchResultsTree = new PostfilterEnabledSearchResultsTree(ConnectionContext.create(
                        ConnectionContext.Category.CATALOGUE,
                        PostfilterEnabledSearchResultsTree.class.getSimpleName()));
        } else {
            searchResultsTree = new SearchResultsTree(ConnectionContext.create(
                        ConnectionContext.Category.CATALOGUE,
                        SearchResultsTree.class.getSimpleName()));
        }

        // AttributePanel ------------------------------------------------------

        attributeViewer = new AttributeViewer();

        // DescriptionPane -----------------------------------------------------
        if (PropertyManager.getManager().getDescriptionPaneHtmlRenderer().equals(
                        PropertyManager.FLYING_SAUCER_HTML_RENDERER)) {
            descriptionPane = new DescriptionPaneFS();
        } else if (PropertyManager.getManager().getDescriptionPaneHtmlRenderer().equals(
                        PropertyManager.FX_HTML_RENDERER)) {
            try {
                descriptionPane = new DescriptionPaneFX();
            } catch (Error e) {
                LOG.error("Error during initialisation of Java FX Description Pane. Using Calpa as fallback.", e);
                descriptionPane = new DescriptionPaneCalpa();
            } catch (Exception e) {
                LOG.error(
                    "Exception during initialisation of Java FX Description Pane. Using Calpa as fallback.",
                    e);
                descriptionPane = new DescriptionPaneCalpa();
            }
        } else {
            descriptionPane = new DescriptionPaneCalpa();
        }
    }

    /**
     * DOCUMENT ME!
     */
    public static void loadLog4JPropertiesFromUrl() {
        if (!l4jinited) {
            try {
                final Properties properties = new Properties();
                final URL log4jPropertiesURL = new URL(log4JUrl);
                InputStream in;

                try {
                    in = WebAccessManager.getInstance().doRequest(log4jPropertiesURL);
                } catch (Exception e) {
                    in = log4jPropertiesURL.openStream();
                }
                try(final InputStream configStream = in) {
                    final ConfigurationSource source = new ConfigurationSource(configStream);
                    final LoggerContext context = (LoggerContext)LogManager.getContext(false);
                    context.start(new XmlConfiguration(context, source)); // Apply new configuration
                }

                l4jinited = true;
            } catch (final Exception e) {
                System.err.println("could not load log4jproperties will try to load it from file" // NOI18N
                            + e.getMessage());
                LOG.error("Cannot load log4j properties. Use default configuration");
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  InterruptedException  DOCUMENT ME!
     */
    private void initUI() throws InterruptedException {
        menuBar = new MutableMenuBar();
        toolBar = new MutableToolBar(propertyManager.isAdvancedLayout());
        container = new LayoutedContainer(toolBar, menuBar, propertyManager.isAdvancedLayout());
        menuBar.registerLayoutManager(container);
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void initDialogs() throws Exception {
        ComponentRegistry.registerComponents(
            null,
            container,
            menuBar,
            toolBar,
            null,
            metaCatalogueTree,
            searchResultsTree,
            workingSpaceTree,
            attributeViewer,
            attributeEditor,
            descriptionPane);
    }

    /**
     * Initialises the configuration manager.
     */
    private void initConfigurationManager() {
        // TODO: Put in method which modifies progress
        String cismapconfig = null;
        String fallBackConfig = null;

        // Default
        if (cismapconfig == null) {
            cismapconfig = "defaultNavigatorProperties.xml"; // NOI18N
        }

        if (fallBackConfig == null) {
            fallBackConfig = "defaultNavigatorProperties.xml"; // NOI18N
        }

        configurationManager.setDefaultFileName(cismapconfig);
        configurationManager.setFallBackFileName(fallBackConfig);

        configurationManager.setFileName("configuration.xml");
        configurationManager.setClassPathFolder("/");
        configurationManager.setFolder(NAVIGATOR_HOME_DIR);
    }

    /**
     * DOCUMENT ME!
     */
    private void checkNavigatorHome() {
        try {
            final File file = new File(NAVIGATOR_HOME);
            if (file.exists()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Navigator Directory exists.");                     // NOI18N
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Navigator Directory does not exist --> creating"); // NOI18N
                }
                file.mkdir();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Navigator Directory successfully created");        // NOI18N
                }
            }
        } catch (Exception ex) {
            LOG.error("Error while checking/creating Navigator home directory", ex); // NOI18N
        }
    }

    /**
     * set the proxy.
     *
     * @throws  ConnectionException   DOCUMENT ME!
     * @throws  InterruptedException  DOCUMENT ME!
     */
    private void initProxy() throws ConnectionException, InterruptedException {
        Proxy proxy = null;
        try {
            proxy = ProxyHandler.getInstance().init(propertyManager.getProxyProperties());
        } catch (final Exception ex) {
            LOG.error("could not initialize Proxy-Settings!", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  InterruptedException  DOCUMENT ME!
     */
    private void initTakeoffHooks() throws InterruptedException {
        final Collection<? extends TakeoffHook> hooks = Lookup.getDefault().lookupAll(TakeoffHook.class);

        for (final TakeoffHook hook : hooks) {
            hook.applicationTakeoff();
        }
    }
}
