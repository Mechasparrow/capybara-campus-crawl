package com.capybara.CapybaraCampusCrawlBackend.Routing;



import java.util.List;

import com.capybara.CapybaraCampusCrawlBackend.Models.Point;

public class CapybaraGraphEdge {
		public List<Point> coords;
		boolean indoors;
		Double distance;
		
		public CapybaraGraphEdge(List<Point> coords, boolean indoors, Double distance) {
			this.coords = coords;
			this.indoors = indoors;
			this.distance = distance;
		}

	}