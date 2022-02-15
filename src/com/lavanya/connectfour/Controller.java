package com.lavanya.connectfour;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Controller implements Initializable {

	private static final int COLUMNS = 7;
	private static final int ROWS = 6;
	private static final int CIRCLE_DIAMETER = 80;
	private static final String discColour1 = "#24303E";
	private static final String discColour2 = "#4CAA88";

	private static String PLAYER_ONE = "Player One";
	private static String PLAYER_TWO = "Player Two";

	private boolean isPlayerOneTurn = true;

	private Disc[][] insertedDiscsArray = new Disc[ROWS][COLUMNS]; //for structural changes for developers

	@FXML
	public GridPane rootGridPane;

	@FXML
	public Pane InsertedDiscsPane;

	@FXML
	public Label playerNameLabel;

	@FXML
	public TextField playerOneTextField;

	@FXML
	public TextField playerTwoTextField;

	@FXML
	public Button setNamesButton;

	private boolean isAllowedToInsert = true; //flag to avoid same color discs being added multiple times when user clicks consecutively

	public void createPlayground(){

		setNamesButton.setOnAction(event -> {
			PLAYER_ONE = playerOneTextField.getText();
			PLAYER_TWO = playerTwoTextField.getText();

			playerNameLabel.setText(isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO);
		});

		Shape rectangleWithHoles = createGameStructuralGrid();
		rootGridPane.add(rectangleWithHoles, 0 ,1);

		List<Rectangle> rectangleList = createClickableColumns();

		for(Rectangle rectangle : rectangleList){
			rootGridPane.add(rectangle, 0, 1);
		}
	}

	private Shape createGameStructuralGrid(){

		Shape rectangleWithHoles = new Rectangle((COLUMNS+1) * CIRCLE_DIAMETER, (ROWS+1) * CIRCLE_DIAMETER);

		for(int row=0 ; row < ROWS ; row++) {

			for (int col = 0; col < COLUMNS; col++) {
				Circle circle = new Circle();
				circle.setRadius(CIRCLE_DIAMETER / 2);
				circle.setCenterX(CIRCLE_DIAMETER / 2);
				circle.setCenterY(CIRCLE_DIAMETER / 2);
				circle.setSmooth(true); //smooth circle

				circle.setTranslateX(col * (CIRCLE_DIAMETER+5) + CIRCLE_DIAMETER / 4);
				circle.setTranslateY(row * (CIRCLE_DIAMETER+5) + CIRCLE_DIAMETER / 4);

				rectangleWithHoles = Shape.subtract(rectangleWithHoles, circle);
			}
		}

		rectangleWithHoles.setFill(Color.WHITE);

		return rectangleWithHoles;
	}

	private List<Rectangle> createClickableColumns(){

		List<Rectangle> rectangleList = new ArrayList<>();

		for(int col=0 ; col<COLUMNS ; col++){

			Rectangle rectangle = new Rectangle(CIRCLE_DIAMETER, (ROWS+1) * CIRCLE_DIAMETER);
			rectangle.setFill(Color.TRANSPARENT);
			rectangle.setTranslateX(col * (CIRCLE_DIAMETER+5) + CIRCLE_DIAMETER / 4);

			rectangle.setOnMouseEntered(event -> rectangle.setFill(Color.valueOf("#eeeeee26")));
			rectangle.setOnMouseExited(event -> rectangle.setFill(Color.TRANSPARENT));

			final int column = col;
			rectangle.setOnMouseClicked(event -> {
				if(isAllowedToInsert) {
					isAllowedToInsert=false;//once anim has started n disc is being dropped no more inserting
					insertDisc(new Disc(isPlayerOneTurn), column);
				}
			});

			rectangleList.add(rectangle);
		}

		return rectangleList;
	}

	private void insertDisc(Disc disc, int column) {

		int row = ROWS-1;
		while(row >= 0){

			if(getDiscIfPresent(row,column) == null)  //empty space found, break and follow insertion process below
				break;

			row--;
		}

		if(row < 0) //col is full cannot insert any discs
			return;

		insertedDiscsArray[row][column] = disc; //for developer structural change
		InsertedDiscsPane.getChildren().add(disc); //for user visual change

		disc.setTranslateX(column * (CIRCLE_DIAMETER+5) + CIRCLE_DIAMETER / 4);

		int currentRow = row;
		TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.5), disc);
		translateTransition.setToY(row * (CIRCLE_DIAMETER+5) + CIRCLE_DIAMETER / 4);
		translateTransition.setOnFinished(event -> {

			isAllowedToInsert=true; //after disc reaches its pos, anim ends, can insert another disc now
			if(gameEnded(currentRow, column)){
				gameOver();
				return; //only 1 player wins, then game ends
			}

			isPlayerOneTurn = !isPlayerOneTurn; //toggle b/w players
			playerNameLabel.setText(isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO);
		});

		translateTransition.play();
	}

	private boolean gameEnded(int row, int column) {


		//Vertical points: ex: player inserted last disc at row =2, col=3
		//range of row values : 0,1,2,3,4,5
		//idx of each elt present in the col [row][col] = 0,3  1,3  2,3  4,3  5,3 ->Class to hold these pts: Point2D

		List<Point2D> verticalPoints = IntStream.rangeClosed(row-3, row+3)       //range of row values : 0,1,2,3,4,5
										.mapToObj(r -> new Point2D(r, column))//returns list of point2d obj
										.collect(Collectors.toList());

		//Horizontal points
		List<Point2D> horizontalPoints = IntStream.rangeClosed(column-3, column+3)       //range of row values : 0,1,2,3,4,5
				.mapToObj(col -> new Point2D(row, col))//returns list of point2d obj
				.collect(Collectors.toList());

		Point2D startPoint1 = new Point2D(row-3, column+3);
		List<Point2D> diagonal1Points = IntStream.rangeClosed(0,6)
				.mapToObj(i -> startPoint1.add(i, -i))
				.collect(Collectors.toList());

		Point2D startPoint2 = new Point2D(row-3, column-3);
		List<Point2D> diagonal2Points = IntStream.rangeClosed(0,6)
				.mapToObj(i -> startPoint2.add(i, i))
				.collect(Collectors.toList());

		boolean isEnded = checkCombinations(verticalPoints)||checkCombinations(horizontalPoints)
							||checkCombinations(diagonal1Points)||checkCombinations(diagonal2Points);

		return isEnded;
	}

	private boolean checkCombinations(List<Point2D> points) {

		int chain = 0; //chain of four

		for(Point2D point : points){

			int rowIndexForArray = (int) point.getX();
			int columnIndexForArray = (int) point.getY();

			Disc disc = getDiscIfPresent(rowIndexForArray,columnIndexForArray);//insertedDiscsArray[rowIndexForArray][columnIndexForArray];

			if(disc != null && disc.isPlayerOneMove == isPlayerOneTurn) { //right combination

				chain++;
				if(chain == 4){
					return true;
				}
			}else{
				chain = 0;
			}
		}

		return false;
	}

	private Disc getDiscIfPresent(int row, int column) { //help avoid array out of bound exception at line insertedDiscArr[rifa][cifa]

		if(row >= ROWS || row < 0 || column >= COLUMNS || column < 0) //invalid row or col
			return null;

		return insertedDiscsArray[row][column];
	}

	private void gameOver() {
		String winner = isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO; //who inserted the last disc is the winner
		System.out.println("Winner is : "+winner);

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Connect4");
		alert.setHeaderText("The winner is "+winner);
		alert.setContentText("Want to play again?");

		ButtonType yesButton = new ButtonType("Yes");
		ButtonType noButton = new ButtonType("No, Exit");
		alert.getButtonTypes().setAll(yesButton, noButton);

		Platform.runLater(() -> {   //so that inside code is exe after anim has ended

			Optional<ButtonType> btnClicked = alert.showAndWait();
			if(btnClicked.isPresent() && btnClicked.get() == yesButton){
				//reset game
				resetGame();
			}else{
				//exit the game
				Platform.exit();
				System.exit(0);
			}
		});
	}

	public void resetGame() { //public to access it in main.java

		InsertedDiscsPane.getChildren().clear(); //remove all inserted discs from pane , visual change
		//structural change, clear insertedDiscsArray
		for(int row=0 ; row<insertedDiscsArray.length; row++){
			for(int col=0 ; col<insertedDiscsArray[row].length ; col++){
				insertedDiscsArray[row][col] = null;
			}
		}

		//now back to player one turn, change label too
		isPlayerOneTurn = true;
		playerNameLabel.setText(PLAYER_ONE);

		createPlayground(); //create fresh playground
	}

	private static class Disc extends Circle {

		private final boolean isPlayerOneMove;

		public Disc(boolean isPlayerOneMove) {

			this.isPlayerOneMove = isPlayerOneMove;
			setRadius(CIRCLE_DIAMETER / 2);
			setFill(isPlayerOneMove ? Color.valueOf(discColour1) : Color.valueOf(discColour2));
			setCenterX(CIRCLE_DIAMETER / 2);
			setCenterY(CIRCLE_DIAMETER / 2);
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}
}
