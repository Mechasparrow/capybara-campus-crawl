package com.capybara.CapybaraCampusCrawlBackend.Routing;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

public class CapybaraGraph extends SimpleDirectedWeightedGraph<Long, CapybaraGraphEdge> {

	public CapybaraGraph(Class<? extends CapybaraGraphEdge> edgeClass) {
		super(edgeClass);
	}

	@Override
	public double getEdgeWeight(CapybaraGraphEdge e) {
		// TODO Auto-generated method stub
		return e.distance;
	}

}
