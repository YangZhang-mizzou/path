package edu.missouri.frame;

import edu.missouri.drone.Drone;
import edu.missouri.drone.static_height.BoustrophedonDrone;
import edu.missouri.drone.variable_height.ImprovedDirectDrone;
import edu.missouri.drone.variable_height.LayerDrone;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class GUIMain {

    private static Area area;
    private static Drone drone;
    private static String[] files = {"Random Pentagon", "Random Dodecagon", "Random Triacontagon", "Random Parallelogram", "small_concave", "cindy_lake", "concave_rect"};
    private static String[] distributors = {"Random", "Clustered"};
    private static Drone[] drones;


    private static JComboBox fileSelect = new JComboBox();
    private static JComboBox droneSelect = new JComboBox();
    private static JComboBox distSelect = new JComboBox();
    private static JTextPane metricsText = new JTextPane();

    private static Thread droneThread  = new Thread(new Runnable() {
        public void run() {
            try {
                drone = drone.getClass().getConstructor(Area.class).newInstance(area);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }, "drone_planning");


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        area = new Area(5);

        drones = new Drone[] {
                new ImprovedDirectDrone(area),
//                new LayerDrone(area),
//                new BoustrophedonDrone(area)
        };

        drone = drones[0];

        JFrame frame = new JFrame("Coverage Path Visualizer");
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        final CoveragePanel cp = new CoveragePanel();

        frame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent windowEvent) { }
            public void windowClosing(WindowEvent windowEvent) { System.exit(0); }
            public void windowClosed(WindowEvent windowEvent) { }
            public void windowIconified(WindowEvent windowEvent) { }
            public void windowDeiconified(WindowEvent windowEvent) { }
            public void windowActivated(WindowEvent windowEvent) { }
            public void windowDeactivated(WindowEvent windowEvent) { }
        });
        cp.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent mouseEvent) {}
            public void mouseMoved(MouseEvent mouseEvent) {
                double scale = Math.max(area.getWidth(), area.getHeight()) / Math.min(cp.getWidth(), cp.getHeight());
                Input.mouseX = (int) ((mouseEvent.getX()*(scale)) - area.toPolygon().leftmost().x());
                Input.mouseY = (int) ((mouseEvent.getY()*(scale)) - area.toPolygon().upmost().y());
            }
        });
        cp.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent mouseEvent) { Input.clicks++;}
            public void mousePressed(MouseEvent mouseEvent) { }
            public void mouseReleased(MouseEvent mouseEvent) { }
            public void mouseEntered(MouseEvent mouseEvent) { }
            public void mouseExited(MouseEvent mouseEvent) { }
        });

        frame.setBackground(new Color(0x0, 0x88, 0x0));

        frame.setLayout(new GridBagLayout());
        frame.setSize(800, 500);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 1.0;
        c.weightx = 0.7;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        frame.add(cp, c);

        Insets simpleInsets = new Insets(5, 5, 5, 5);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weighty = 1.0;
        c.gridheight = 1;
        c.insets = simpleInsets;
        c.fill = GridBagConstraints.BOTH;
        JTabbedPane tabs = new JTabbedPane();
        frame.add(tabs, c);

        JPanel generalPanel = new JPanel();
        generalPanel.setLayout(new GridBagLayout());
        JPanel metricsPanel = new JPanel();
        metricsPanel.setLayout(new GridBagLayout());
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridBagLayout());
        JPanel dronePanel   = new JPanel();
        dronePanel.setLayout(new GridBagLayout());

        tabs.addTab("Metrics", metricsPanel);
        tabs.addTab("Options", optionsPanel);
        tabs.addTab("Drone",   dronePanel);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.insets = simpleInsets;
        frame.add(generalPanel, c);

        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0.1;
        c.insets = simpleInsets;
        JLabel mapLabel = new JLabel("Map: ");
        mapLabel.setHorizontalAlignment(JLabel.RIGHT);
        generalPanel.add(mapLabel, c);

        c.gridx = 1;
        c.weightx = 0.9;
        c.fill = GridBagConstraints.HORIZONTAL;
        for(String s: files) fileSelect.addItem(s);
        generalPanel.add(fileSelect, c);

        ActionListener refresh = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String filename = (String) fileSelect.getSelectedItem();
                if(filename == null) return;
                switch (filename) {
                    case "Random Pentagon": { area = new Area(5); break; }
                    case "Random Dodecagon": { area = new Area(12); break; }
                    case "Random Triacontagon": { area = new Area(30); break; }
                    case "Random Parallelogram": { area = Area.randomParallelogram(); break; }
                    default: { area = new Area("map/" + filename + ".png", 0.95); }
                }

                droneThread  = new Thread(new Runnable() {
                    public void run() {
                        try {
                            drone = drone.getClass().getConstructor(Area.class).newInstance(area);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        cp.repaint();
                    }
                }, "drone_planning");
                droneThread.start();
                cp.repaint();
            }
        };

        fileSelect.addActionListener(refresh);

        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0.1;
        c.fill = GridBagConstraints.BOTH;
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(refresh);
        generalPanel.add(refreshButton);


        c = new GridBagConstraints();
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0.1;
        c.insets = simpleInsets;

        JLabel droneLabel = new JLabel("Drone: ");
        droneLabel.setHorizontalAlignment(JLabel.RIGHT);
        generalPanel.add(droneLabel, c);

        c.gridx = 1;
        c.weightx = 0.9;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        for(Drone d: drones) droneSelect.addItem(d);
        generalPanel.add(droneSelect, c);

        droneSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                drone = (Drone) droneSelect.getSelectedItem();
                droneThread  = new Thread(new Runnable() {
                    public void run() {
                        try {
                            drone = drone.getClass().getConstructor(Area.class).newInstance(area);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        cp.repaint();
                    }
                }, "drone_planning");
                droneThread.start();
                cp.repaint();
            }
        });

        c = new GridBagConstraints();
        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0.1;
        c.insets = simpleInsets;

        JLabel distLabel = new JLabel("Spread: ");
        distLabel.setHorizontalAlignment(JLabel.RIGHT);
        generalPanel.add(distLabel, c);

        c.gridx = 1;
        c.weightx = 0.9;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        for(String s: distributors) distSelect.addItem(s);
        generalPanel.add(distSelect, c);

        distSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Object o = distSelect.getSelectedItem();
                if(o != null && o.equals("Random"))    Option.distributor = Area.RANDOM;
                if(o != null && o.equals("Clustered")) Option.distributor = Area.CLUSTERED;
                area.redistribute();

                droneThread  = new Thread(new Runnable() {
                    public void run() {
                        try {
                            drone = drone.getClass().getConstructor(Area.class).newInstance(area);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        cp.repaint();
                    }
                }, "drone_planning");
                droneThread.start();
                cp.repaint();
            }
        });

        c = new GridBagConstraints();

        c.fill = GridBagConstraints.NONE;
        c.fill = GridBagConstraints.VERTICAL;
        metricsPanel.add(metricsText, c);


        metricsText.setEditable(false);
        metricsText.setContentType("text/html");
        metricsText.setFont(fileSelect.getFont());
        metricsText.setBackground((new JLabel()).getBackground());

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        optionsPanel.add(new JLabel("Starting altitude:  "), c);

        final JSpinner altitudeSpinner = new JSpinner(new SpinnerNumberModel(Option.cruiseAltitude, 10, Option.maxAltitude, 1));
        altitudeSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                Option.cruiseAltitude = (Double) altitudeSpinner.getValue();
                droneThread  = new Thread(new Runnable() {
                    public void run() {
                        try {
                            drone = drone.getClass().getConstructor(Area.class).newInstance(area);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        cp.repaint();
                    }
                }, "drone_planning");
                droneThread.start();
                cp.repaint();
            }
        });
        c.gridx = 1;
        optionsPanel.add(altitudeSpinner, c);

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridy = 2;
        optionsPanel.add(new JLabel("Number of objects:  "), c);

        final JSpinner numObjectsSpinner = new JSpinner(new SpinnerNumberModel(Option.numObjects, 1, 500, 1));
        numObjectsSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                Option.numObjects = (int) numObjectsSpinner.getValue();
                area.redistribute();
                droneThread  = new Thread(new Runnable() {
                    public void run() {
                        try {
                            drone = drone.getClass().getConstructor(Area.class).newInstance(area);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        cp.repaint();
                    }
                }, "drone_planning");
                droneThread.start();
                cp.repaint();
            }
        });
        c.gridx = 1;
        optionsPanel.add(numObjectsSpinner, c);

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridy = 3;
        optionsPanel.add(new JLabel("Confidence threshold:  "), c);

        final JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(Option.confidenceThreshold, 0.0, 1.0, 0.05));
        thresholdSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                Option.confidenceThreshold = (double) thresholdSpinner.getValue();
                droneThread  = new Thread(new Runnable() {
                    public void run() {
                        try {
                            drone = drone.getClass().getConstructor(Area.class).newInstance(area);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        cp.repaint();
                    }
                }, "drone_planning");
                droneThread.start();
                cp.repaint();
            }
        });
        c.gridx = 1;
        optionsPanel.add(thresholdSpinner, c);

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridy = 4;
        optionsPanel.add(new JLabel("Allotted energy:  "), c);

        final JSpinner energySpinner = new JSpinner(new SpinnerNumberModel(Option.energyBudget, 0, 200000, 1000));
        energySpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                Option.energyBudget = (double) energySpinner.getValue();
                droneThread  = new Thread(new Runnable() {
                    public void run() {
                        try {
                            drone = drone.getClass().getConstructor(Area.class).newInstance(area);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        cp.repaint();
                    }
                }, "drone_planning");
                droneThread.start();
                cp.repaint();
            }
        });
        c.gridx = 1;
        optionsPanel.add(energySpinner, c);

        cp.repaint();
        frame.setVisible(true);
    }

    private static String getMetricsString() {
        double d = drone.energyUsed();
        return "<html>" +
                String.format("<b>Total path length:</b> %.2fm", drone.length()) +
                String.format("<br><b>Total angular length:</b> %.2f deg", drone.angularLength()) +
                String.format("<br><b>Percent covered:</b> %.1f%%", drone.areaCovered()*100) +
                String.format("<br><b>Avg. confidence:</b> %.1f%%", drone.sumConfidences()*100) +
                "<br>" +
                String.format("<br><b>Accuracy:</b> %.1f%%", drone.objectAccuracy()*100) +
                String.format("<br><b>Precision:</b> %.1f%%", drone.objectPrecision()*100) +
                String.format("<br><b>Recall:</b> %.1f%%", drone.objectRecall()*100) +
                String.format("<br><b>Split decisions:</b> %d", drone.splitDecisions()) +
                "<br>" +
                ((d < Drone.TOTAL_ENERGY)?  "<div style=\"color:Green;\">" : "<p style=\"color:Red;\">") +
                String.format("<br><b>Energy used:</b> %.1f kJ", d/1000) +
                "</div>" +
                "</html>";
    }


    static class CoveragePanel extends JPanel {

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(new Color(0xff, 0xff, 0xff));
            g.setFont(new Font("Helvetica", Font.PLAIN, 18));
            g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g2d = (Graphics2D) g;


            double scale = Math.max(area.getWidth(), area.getHeight()) / Math.min(getWidth(), getHeight());
            g2d.scale(1/scale, 1/scale);
            g2d.translate(area.toPolygon().leftmost().ix(), area.toPolygon().upmost().iy());
            area.draw(g2d);

            if(! droneThread.isAlive()) drone.draw(g2d);

            g2d.scale(scale, scale);
            if(droneThread.isAlive()) g.drawString("Planning...", 20, 20);

            metricsText.setText(getMetricsString());
            metricsText.repaint();
        }
    }
}