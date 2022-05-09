package com.capybara.CapybaraCampusCrawlBackend.BusinessLogic;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.capybara.CapybaraCampusCrawlBackend.Controllers.BuildingsApiController;
import com.capybara.CapybaraCampusCrawlBackend.Models.Building;
import com.capybara.CapybaraCampusCrawlBackend.Models.BuildingRouteRequest;
import com.capybara.CapybaraCampusCrawlBackend.Models.GraphNode;
import com.capybara.CapybaraCampusCrawlBackend.Models.Location;
import com.capybara.CapybaraCampusCrawlBackend.Models.PitstopConstraint;
import com.capybara.CapybaraCampusCrawlBackend.Models.Point;
import com.capybara.CapybaraCampusCrawlBackend.Models.RouteRequest;
import com.capybara.CapybaraCampusCrawlBackend.Models.RouteRequestConstraints;
import com.capybara.CapybaraCampusCrawlBackend.Routing.RoutingSystem;

@Component
public class RoutingBll {

	private RoutingSystem routingDao;
	
	private BuildingBll buildingBll;
	
	@Autowired
	public RoutingBll(RoutingSystem routingDao, BuildingBll buildingBll) {
		this.routingDao = routingDao;
		this.buildingBll = buildingBll;
	}
	
	private static boolean hasNoConstraints(RouteRequestConstraints constraints) {
    	return constraints.getPreferIndoors() == false 
                && constraints.getStopForFood() == false 
                && constraints.getAvoidCrowds() == false
                && constraints.getPitstops().size() == 0
                && !constraints.getTimeConstraint().isPresent();
    }
    
    private static void LogConstraints(RouteRequestConstraints constraints) {
    	 System.out.println("Prefer Indoors: " + constraints.getPreferIndoors());
         System.out.println("Stop by food: " + constraints.getStopForFood());
         System.out.println("Avoid Crouds:" + constraints.getAvoidCrowds());
         
         List<PitstopConstraint> pitstops = constraints.getPitstops();
         
         if (pitstops.size() > 0) {
             Location location = pitstops.get(0).getLocation();
             
             System.out.println("Building ID: " + location.getBuildingId());
             System.out.println("Building Lat: " + location.getLatitude());
             System.out.println("Building Long: " + location.getLongitude());
         }
         
         if (constraints.getTimeConstraint().isPresent()) {
             System.out.println("Max Time: "+ constraints.getTimeConstraint().get().getMaxTime());
         }else {
         	System.out.println("Max Time: "+ "null");
         }
    }
	
    private List<Pair<GraphNode, GraphNode>> buildUpRoutingPairs(GraphNode startingNode, List<PitstopConstraint> intermediatePitstops, GraphNode endingNode){
    	List<GraphNode> intermediateNodes = intermediatePitstops.stream()
    			.map(pitstopConstraint -> pitstopConstraint.getLocation())
    			.map(location -> location.getBuildingId())
    			.map(buildingId -> buildingBll.fetchBuildingGraphNode(buildingId))
    			.collect(Collectors.toList());
    	
    	List<GraphNode> routingNodes = new ArrayList<>(intermediateNodes);
		routingNodes.add(0, startingNode);
		routingNodes.add(endingNode);
    	
    	List<Pair<GraphNode,GraphNode>> routingPairs = new ArrayList<>();
		GraphNode previousNode = routingNodes.get(0);
			
		for (int i = 1; i < routingNodes.size(); i++) {
			GraphNode currentNode = routingNodes.get(i);
			
			MutablePair<GraphNode, GraphNode> routingPair = new MutablePair<>();
			routingPair.setLeft(previousNode);
			routingPair.setRight(currentNode);
			
			routingPairs.add(routingPair);
			
			previousNode = currentNode;
		}
		
		return routingPairs;
    }
    
	public List<Point> fetchRoute(RouteRequest routeRequest){
		RouteRequestConstraints constraints = routeRequest.getConstraints();
		LogConstraints(constraints);

    	GraphNode graphStartingNode = buildingBll.fetchBuildingGraphNode(routeRequest.getFromLocation().getBuildingId());
		GraphNode graphFinalNode = buildingBll.fetchBuildingGraphNode(routeRequest.getToLocation().getBuildingId());
		
		//determine routing pairs
		List<Pair<GraphNode,GraphNode>> routingPairs = buildUpRoutingPairs(graphStartingNode, constraints.getPitstops(), graphFinalNode);
				
		List<Point> points = new ArrayList<Point>();
		
		for (Pair<GraphNode, GraphNode> routingPair : routingPairs) {
			try {
				boolean preferIndoors = constraints.getPreferIndoors();
				List<Point> routingPoints = routingDao.ComputeRoute(routingPair.getLeft().getNodeID(), routingPair.getRight().getNodeID(), preferIndoors);
				points.addAll(routingPoints);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		return points;
	}
	
	public List<Point> fetchRoute(BuildingRouteRequest buildingRouteRequest){
		RouteRequest routeRequest = new RouteRequest();
		
		Location fromLocation = new Location();
		fromLocation.setBuildingId(buildingRouteRequest.getFromBuilding().getBuildingId());
		
		Location toLocation = new Location();
		toLocation.setBuildingId(buildingRouteRequest.getToBuilding().getBuildingId());
				
		RouteRequestConstraints nullConstraints = new RouteRequestConstraints();
		nullConstraints.setAvoidCrowds(false);
		nullConstraints.setPreferIndoors(false);
		nullConstraints.setStopForFood(false);
		nullConstraints.setPitstops(new ArrayList<>());
		nullConstraints.setTimeConstraint(Optional.empty());
		
		routeRequest.setFromLocation(fromLocation);
		routeRequest.setToLocation(toLocation);
		routeRequest.setConstraints(nullConstraints);
		return fetchRoute(routeRequest);
	}
}
