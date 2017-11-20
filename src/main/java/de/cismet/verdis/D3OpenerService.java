/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.verdis;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import de.cismet.remote.RESTRemoteControlStarter;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public class D3OpenerService {

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */

    public static void main(final String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Clould not set LnF. Sad.");
        }
        if (SystemTray.isSupported()) {
            System.out.println("System Tray Supported");
            final SystemTray systemTray = SystemTray.getSystemTray();
            String error = "";
            String image = "/verdis/d3.png";
            int port = 3033;
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception e) {
                error = " (" + e.getMessage() + ". going to use default port " + port + ")";
            }
            try {
                RESTRemoteControlStarter.initRestRemoteControlMethods(port);
            } catch (Exception e) {
                image = "/verdis/d3bw.png";
                error = " (" + e.getMessage() + ")";
            }

            final TrayIcon trayIcon = new TrayIcon(new javax.swing.ImageIcon(
                        D3OpenerService.class.getResource(image)).getImage(),
                    "verdis d.3 Helper"
                            + error);
            trayIcon.setImageAutoSize(true);
            final PopupMenu popup = new PopupMenu();

            final MenuItem aboutItem = new MenuItem("Über");
            final MenuItem exitItem = new MenuItem("Beenden");

            // Add components to pop-up menu
            popup.add(aboutItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);

            aboutItem.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        JOptionPane.showMessageDialog(null,
                            "Öffnen der d.3 Suche aus verdis2go.");
                    }
                });
            exitItem.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        System.exit(0);
                    }
                });
            try {
                systemTray.add(trayIcon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
