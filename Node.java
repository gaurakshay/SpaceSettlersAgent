package gaur4004;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.gui.JSpaceSettlersComponent;
import spacesettlers.utilities.Position;

/**
 * This class is used to store the status of a Node
 * in the Grid that is used to implement A* algorithm.
 *
 */
public class Node extends SpacewarGraphics {
	
	// Position details of the node.
	private Position drawLocation;
	private double x;
	private double y;
	private Rectangle2D rect;
	
	// State of the node.
	private boolean occupied;
	private boolean partOfPath;
	private boolean visited;
	
	// Parent node when the node is part of a path.
	private Node parentNode;

	// Node colors
	private Color DEFAULT_COLOR = new Color(0, 0, 255, 50);
	private Color OCCUPIED_COLOR = new Color(255, 0, 0, 80);
	private Color PATH_COLOR = new Color(0, 255, 0, 80);
	private Color color;
	
	// Cost associated with the node.
	private double gCost;
	private double hCost;
	
	/**
	 * @param _occupied
	 * @param _position
	 */
	Node(int _size, int _x, int _y){
		super(_size, _size);
		drawLocation = new Position(_x, _y);
		x = _x;
		y = _y;
		occupied = false;
		partOfPath = false;
		color = null;
		gCost = Double.MAX_VALUE;
		hCost = Double.MAX_VALUE;
		rect = new Rectangle2D.Double(_x, _y, _size, _size);
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	/**
	 * @return the occupied
	 */
	public boolean isOccupied() {
		return occupied;
	}

	/**
	 * @param occupied the occupied to set
	 */
	public void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

	/**
	 * @return the visited
	 */
	public boolean isVisited() {
		return visited;
	}

	/**
	 * @param visited the visited to set
	 */
	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	/**
	 * @return the gCost
	 */
	public double getgCost() {
		return gCost;
	}

	/**
	 * @param gCost the gCost to set
	 */
	public void setgCost(double gCost) {
		this.gCost = gCost;
	}

	/**
	 * @return the hCost
	 */
	public double gethCost() {
		return hCost;
	}

	/**
	 * @param hCost the hCost to set
	 */
	public void sethCost(double hCost) {
		this.hCost = hCost;
	}

	/**
	 * @return the color
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * @return the partOfPath
	 */
	public boolean isPartOfPath() {
		return partOfPath;
	}

	/**
	 * @param partOfPath the partOfPath to set
	 */
	public void setPartOfPath(boolean partOfPath) {
		this.partOfPath = partOfPath;
	}

	public Node getParentNode() {
		return parentNode;
	}

	public void setParentNode(Node parentNode) {
		this.parentNode = parentNode;
	}

	/**
	 * Total cost associated with this node.
	 * @return
	 */
	public double getfCost() {
		return getgCost() + gethCost();
	}

	@Override
	public Position getActualLocation() {
		return drawLocation;
	}

	@Override
	public void draw(Graphics2D graphics) {
		setColor(graphics);
		if(graphics.getColor() != DEFAULT_COLOR) {
		    graphics.fill(rect);
		    graphics.setStroke(JSpaceSettlersComponent.THIN_STROKE);
		    graphics.draw(rect);
		}
	}
	
	private void setColor(Graphics2D graphics){
		if(this.color != null) {
			graphics.setColor(color);
		} else if (this.isOccupied()) {
			graphics.setColor(OCCUPIED_COLOR);
		} else if (this.isPartOfPath()) {
			graphics.setColor(PATH_COLOR);
		} else {
			graphics.setColor(DEFAULT_COLOR);
		}
	}

	@Override
	public boolean isDrawable() {
		return true;
	}

	public boolean contains(double _x, double _y) {
		if (rect.contains(_x, _y)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean contains(Position pos) {
		if (rect.contains(pos.getX(), pos.getY())) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean intersects(Rectangle2D _rect) {
		if (rect.intersects(_rect)) {
			return true;
		} else {
			return false;			
		}
	}
	
	public Rectangle2D getRect() {
		return rect;
	}

}