package it.polito.tdp.itunes.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jgrapht.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import it.polito.tdp.itunes.db.ItunesDAO;

public class Model {
	
	private List<Album> allAlbum;
	private SimpleDirectedWeightedGraph<Album, DefaultWeightedEdge> graph;
	private ItunesDAO dao;
	private List<Album> bestPath;
	private Integer bestScore;
	
	public Model() {
		this.allAlbum = new ArrayList<Album>();
		this.graph = new SimpleDirectedWeightedGraph<Album, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		this.dao = new ItunesDAO();
	}
	
	public void createGraph(Integer nSongs) {
		
		this.clearGraph();
		
		this.loadNodes(nSongs);
		
		for (Album a1 : this.allAlbum) {
			for (Album a2 : this.allAlbum) {
				Integer weight = a1.getnSongs() - a2.getnSongs();
				if (weight > 0) {
					Graphs.addEdgeWithVertices(this.graph, a2, a1, weight);
				}
			}
		}
		
		System.out.println(this.graph.vertexSet().size());
		System.out.println(this.graph.edgeSet().size());
		
	}
	
	private void clearGraph() {
		this.allAlbum = new ArrayList<Album>();
		this.graph = new SimpleDirectedWeightedGraph<Album, DefaultWeightedEdge>(DefaultWeightedEdge.class); 	
	}

	public void loadNodes(Integer nSongs) {
		if (this.allAlbum.isEmpty()) {
			this.allAlbum = dao.getFilteredAlbums(nSongs);
		}
	}
	
	private double getBilancio(Album a) {
		
		List<DefaultWeightedEdge> in = new ArrayList<>(this.graph.incomingEdgesOf(a));
		List<DefaultWeightedEdge> out = new ArrayList<>(this.graph.outgoingEdgesOf(a));
		
		double bilancio = 0;
		
		for (DefaultWeightedEdge dfeIn : in) {
			bilancio += this.graph.getEdgeWeight(dfeIn); 
		}
		
		for (DefaultWeightedEdge dfeOut : out) {
			bilancio -= this.graph.getEdgeWeight(dfeOut); 
		}
		
		return bilancio;
	}
	
	public  List<BilancioAlbum> getAdiacenti(Album a){
		List<Album> successori = Graphs.successorListOf(this.graph, a);
		List<BilancioAlbum>  result = new ArrayList<BilancioAlbum>();
		for (Album succ : successori) {
			result.add(new BilancioAlbum(succ, this.getBilancio(a)));
		}
		Collections.sort(result);
		return result;
	}
	
	public List<Album> getCammino(Album partenza, Album arrivo, Integer threshold){
		
		List<Album> soluzioneParziale = new ArrayList<Album>();
		this.bestPath = new ArrayList<Album>();
		this.bestScore = 0;
		soluzioneParziale.add(partenza);
		
		this.ricorsione(soluzioneParziale, arrivo, threshold);
		
		return this.bestPath;
	}
	
	public void ricorsione(List<Album> soluzioneParziale, Album target, Integer threshold) {
		Album current = soluzioneParziale.get(soluzioneParziale.size() - 1);
		
		//Condizione di uscita
		if (current.equals(target)) {
			if (getScore(soluzioneParziale) > this.bestScore) {
				this.bestScore = this.getScore(soluzioneParziale);
				this.bestPath = new ArrayList<>(soluzioneParziale); //ATTENTO: DEVI FARE UNA COPIA DELLA PARZIALE
															//E NON METTERCI IL RIFERIMENTO (PARZIALE MUTA NELLA RICORSIONE)
			}
		}
		else { //CONTINUO AD AGGIUNGERE IN PARZIALE
			List<Album> successors = Graphs.successorListOf(this.graph, current);
			
			for (Album a : successors) {
				if (this.graph.getEdgeWeight(this.graph.getEdge(current, a)) >= threshold){
					soluzioneParziale.add(a);
					ricorsione (soluzioneParziale, target, threshold);
					soluzioneParziale.remove(soluzioneParziale.size()-1); //SOLITO BACKTRACKING
				}
			}
		}
	}

	private Integer getScore(List<Album> soluzioneParziale) {
		Integer score = 0;
		Album source = soluzioneParziale.get(0);
		for (Album a : soluzioneParziale.subList(1, soluzioneParziale.size())) {
			if (this.getBilancio(a) > this.getBilancio(source)) {
				score++;
			}
		}
		return score;
	}
}
