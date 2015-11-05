/*******************************************************************************
 *
 * Copyright (c) 2004-2011, Oracle Corporation
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Kohsuke Kawaguchi,  Winston.Prakash@Oracle.com
 *
 *******************************************************************************/
package org.eclipse.hudson.graph;

import hudson.model.Descriptor.FormException;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletOutputStream;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Calendar;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A Graph interface for graphing support. Actual graph generation is delegated
 * to GraphSupport object. Plugin can provide their own graphing support via
 * GraphSupport Extension Point.
 *
 * <p> This object exposes two URLs: <dl> <dt>/png <dd>PNG image of a graph
 *
 * <dt>/map <dd>Clickable map </dl>
 *
 * @since 2.3.0
 */
public class Graph {

    public static final int TYPE_STACKED_AREA = 1;
    public static final int TYPE_LINE = 2;
    public static final int TYPE_STACKED_BAR = 3;
    private final long timestamp;
    private int width;
    private int height;
    private volatile GraphSupport graphSupport;

    /**
     * @param timestamp Timestamp of this graph. Used for HTTP cache related
     * headers. If the graph doesn't have any timestamp to tie it to, pass -1.
     */
    public Graph(long timestamp, int defaultW, int defaultH) {
        this.timestamp = timestamp;
        this.width = defaultW;
        this.height = defaultH;
        if (!GraphSupport.all().isEmpty()) {
            try {
                graphSupport = GraphSupport.all().get(0).newInstance(null, null);
            } catch (FormException ex) {
                ex.printStackTrace();
            }
        }
    }

    public Graph(Calendar timestamp, int defaultW, int defaultH) {
        this(timestamp.getTimeInMillis(), defaultW, defaultH);
    }

    public synchronized GraphSupport getGraphSupport() {
        return graphSupport;
    }

    public void setGraphSupport(GraphSupport graphSupport) {
        this.graphSupport = graphSupport;
    }

    public void setChartType(int chartType) {
        if (graphSupport != null) {
            graphSupport.setChartType(chartType);
        }
    }

    public void setMultiStageTimeSeries(List<MultiStageTimeSeries> multiStageTimeSeries) {
        if (graphSupport != null) {
            graphSupport.setMultiStageTimeSeries(multiStageTimeSeries);
        }
    }

    public void setData(DataSet data) {
        if (graphSupport != null) {
            graphSupport.setData(data);
        }
    }

    public void setTitle(String title) {
        if (graphSupport != null) {
            graphSupport.setTitle(title);
        }
    }

    public void setXAxisLabel(String xLabel) {
        if (graphSupport != null) {
            graphSupport.setXAxisLabel(xLabel);
        }
    }

    public void setYAxisLabel(String yLabel) {
        if (graphSupport != null) {
            graphSupport.setYAxisLabel(yLabel);
        }
    }

    public BufferedImage createImage(int width, int height) {
        BufferedImage image = null;
        if (graphSupport != null) {
            image = graphSupport.render(width, height);
        } else {
            image = createErrorImage(width, height);
        }
        return image;
    }

    /**
     * Renders the graph.
     */
    public void doPng(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (req.checkIfModified(timestamp, rsp)) {
            return;
        }

        try {
            String w = req.getParameter("width");
            if (w != null) {
                width = Integer.parseInt(w);
            }
            String h = req.getParameter("height");
            if (h != null) {
                height = Integer.parseInt(h);
            }
            rsp.setContentType("image/png");
            ServletOutputStream os = rsp.getOutputStream();
            BufferedImage image = createImage(width, height);
            ImageIO.write(image, "PNG", os);
            os.close();
        } catch (Error e) {
            /* OpenJDK on ARM produces an error like this in case of headless error
             Caused by: java.lang.Error: Probable fatal error:No fonts found.
             at sun.font.FontManager.getDefaultPhysicalFont(FontManager.java:1088)
             at sun.font.FontManager.initialiseDeferredFont(FontManager.java:967)
             ..
             */
            if (e.getMessage().contains("Probable fatal error:No fonts found")) {
                rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
                return;
            }
            throw e; // otherwise let the caller deal with it
        } catch (HeadlessException e) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
        }
    }

    /**
     * Create a Image map with the given name, width and height
     *
     * @param mapName
     * @param width
     * @param height
     * @return
     */
    public String createImageMap(String mapName, int width, int height) {
        return graphSupport.getImageMap(mapName, width, height);
    }

    /**
     * Send the a clickable map data information.
     */
    public void doMap(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (graphSupport != null) {
            if (req.checkIfModified(timestamp, rsp)) {
                return;
            }

            String w = req.getParameter("width");
            if (w != null) {
                width = Integer.parseInt(w);
            }
            String h = req.getParameter("height");
            if (h != null) {
                height = Integer.parseInt(h);
            }
            rsp.setContentType("text/plain;charset=UTF-8");
            String mapHtml = createImageMap("map", width, height);
            rsp.getWriter().println(mapHtml);
        }
    }

    private BufferedImage createErrorImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        graphics.setColor(ColorPalette.RED);
        Font font = new Font("Serif", Font.BOLD, 14);
        graphics.drawRect(2, 2, width - 4, height - 4);
        graphics.drawString("Graph Support missing. \n Install Graph Support Plugin", 10, height / 2);
        return img;
    }
}
