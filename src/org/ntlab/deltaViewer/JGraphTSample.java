/*
 * (C) Copyright 2013-2018, by Barak Naveh and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package org.ntlab.deltaViewer;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.*;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxLine;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import org.jgrapht.ext.*;
import org.jgrapht.graph.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * A demo applet that shows how to use JGraphX to visualize JGraphT graphs. Applet based on
 * JGraphAdapterDemo.
 *
 */
public class JGraphTSample extends JApplet {
    private static final Dimension DEFAULT_SIZE = new Dimension(530, 320);
    private JGraphXAdapter<String, DefaultEdge> jgxAdapter;

    /**
     * An alternative starting point for this demo, to also allow running this applet as an
     * application.
     *
     * @param args Command line arguments.
     */
    /**
     * @param args
     */
    public static void main(String[] args)
    {
    	//Build a frame, create a graph, and add the graph to the frame so you can actually see the graph.
    	JFrame frame = new JFrame("Branching graph");
    	frame.setSize(500, 500);
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    	mxGraph mxgraph= new mxGraph();
    	mxGraphComponent graphComponent= new mxGraphComponent(mxgraph);

    	frame.add(graphComponent, BorderLayout.CENTER);
    	frame.setVisible(true);

    	//No clue what this does but it is needed.
    	Object mxDefaultParent = mxgraph.getDefaultParent();

    	//Add a vertex to the graph in a transactional fashion. The vertex is actually a 'cell' in jgraphx terminology.
    	Object vertex=null;
    	Object A=null;
    	Object B=null;
    	Object C=null;
    	Object D=null;
    	Object E=null;

    	Object Am=null;
    	Object Bm=null;
    	Object Dm=null;
    	Object Em=null;
    	
    	Object b=null;
    	Object c=null;
    	Object d=null;
    	Object e=null;
    	
    	Object AmE=null;
    	Object BmE=null;
    	Object DmE=null;
    	mxgraph.getModel().beginUpdate();
    	try{
    	  double xCor=225;
    	  double yCor=100.0;
    	  double width=70;
    	  double height=70;
    	  
    	  A=mxgraph.insertVertex(mxDefaultParent, "A", "A", xCor, yCor, width , height,"fillColor=blue"); //creates a blue vertex 
    	  B=mxgraph.insertVertex(mxDefaultParent, "B", "B", xCor-100, yCor+100, width, height,"fillColor=blue");
    	  C=mxgraph.insertVertex(mxDefaultParent, "C", "C", xCor-200, yCor+200, width , height,"fillColor=blue");
    	  D=mxgraph.insertVertex(mxDefaultParent, "D", "D", xCor+100, yCor+100, width , height,"fillColor=blue");
    	  E=mxgraph.insertVertex(mxDefaultParent, "E", "E", xCor+200 , yCor+200, width, height,"fillColor=blue");
    	  
    	  Am=mxgraph.insertVertex(mxDefaultParent, "Am", "A.m()", getXForCell(mxgraph, "A")+10, yCor+40, width-15 , height-40,"fillColor=blue; alignVertical=middle"); //creates a blue vertex 
    	  Bm=mxgraph.insertVertex(mxDefaultParent, "Bm", "B.getC()", getXForCell(mxgraph, "B")+10, yCor+140, width-15, height-50,"fillColor=blue");
    	  Dm=mxgraph.insertVertex(mxDefaultParent, "Dm", "D.passB()", getXForCell(mxgraph, "D")+10, yCor+140, width-15 , height-50,"fillColor=blue");
    	  Em=mxgraph.insertVertex(mxDefaultParent, "Em", "E.setC()", getXForCell(mxgraph, "E")+10 , yCor+240, width-15, height-50,"fillColor=blue");

    	  b = mxgraph.insertEdge(mxDefaultParent, "b", "b", A, B, "edgeStyle=elbowEdgeStyle;elbow=horizontal;"
					+ "exitX=0.5;exitY=1;exitPerimeter=1;entryX=-10;entryY=-10;entryPerimeter=1;");
    	  c = mxgraph.insertEdge(mxDefaultParent, "c", "c", B, C);
    	  d = mxgraph.insertEdge(mxDefaultParent, "d", "d", A, D);
    	  e = mxgraph.insertEdge(mxDefaultParent, "e", "e", D, E);
    	  
    	  AmE = mxgraph.insertEdge(mxDefaultParent, "AmE", "", Am, Bm);
    	  BmE = mxgraph.insertEdge(mxDefaultParent, "BmE", "", Bm, Dm);
    	  DmE = mxgraph.insertEdge(mxDefaultParent, "DmE", "", Dm, Em);

    	  final Graphics2D g = (Graphics2D)graphComponent.getGraphics();
//      	  mxGraphics2DCanvas canvas = new mxGraphics2DCanvas(g);
//      	  canvas.paintPolyline(new mxPoint[] {new mxPoint(100,100),new mxPoint(500,500)}, true);
      	Runnable r = new Runnable() {
      	   public void run() {
      	      g.setColor(Color.GREEN);
      	    Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
            g.setStroke(dashed);
      	      g.drawLine(0, 0, 500, 500);
      	   }
      	};

      	if (!SwingUtilities.isEventDispatchThread()) {
      	    SwingUtilities.invokeLater(r);
      	} else {
      	    r.run();
      	}


		}
    	finally{
    	  mxgraph.getModel().endUpdate();
    	}

    	/*Given a cell, we can change it's style attributes, for example the color. NOTE that you have to call the graphComponent.refresh() function, otherwise you won't see the difference!*/
    	mxgraph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "white", new Object[]{A, B, C, D, E, Am, Bm, Dm, Em}); //changes the color to red
    	mxgraph.setCellStyles(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE, new Object[]{A, B, C, D, E});
    	mxgraph.setCellStyles(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_ELLIPSE, new Object[]{A, B, C, D, E});
    	mxgraph.setCellStyles(mxConstants.STYLE_EDGE, mxConstants.SHAPE_CURVE, new Object[] {AmE, BmE, DmE, b, c, d, e});
    	mxgraph.setCellStyleFlags(mxConstants.STYLE_DASHED, 1, true, new Object[] {AmE, BmE, DmE});
  	// Adds animation to edge shape and makes "pipe" visible
		((mxCell)b).setAttribute("path", ".flow {" 
				  + "stroke-dasharray: 8;"
				  + "animation: dash 0.5s linear;"
				  + "animation-iteration-count: infinite;"
				  + "}"
				  + "@keyframes dash {"
				 + "to {"
				  +  "stroke-dashoffset: -16;"
				  + "}");

    	graphComponent.refresh();      	  

    }
    
    private static double getXForCell(mxGraph graph, String id) {
        double res = -1;
        graph.clearSelection();
        graph.selectAll();
        Object[] cells = graph.getSelectionCells();
        for (Object object : cells) {
            mxCell cell = (mxCell) object;
            if (id.equals(cell.getId())) {
                res = cell.getGeometry().getX();
            }
        }
        graph.clearSelection();
        return res;
    }
    
    class GPanel extends JPanel {

    	  @Override
    	  public void paintComponent(Graphics g) {
    	    super.paintComponent(g);
    	    Graphics2D g2 = (Graphics2D)g;
    	    int w = this.getWidth();
    	    int h = this.getHeight();
    	    for(int i = 0;i < 10;i++){
    	      Ellipse2D shape = new Ellipse2D.Double(0,0,w,h - i * (w / 10));
    	      g2.setPaint(new Color(0,0,255,25));
    	      g2.fill(shape);
    	    }
    	  }
    }
}