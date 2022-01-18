import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class tetris {
	//Constants
	public int _boardBuffer = 2;
	public int _boardWidth = 10, _boardHeight = 20 + _boardBuffer;
	int _fallTime = 1000;
	int _pieceWidth = 4;
	int _pieceHeight = 3;
	
	//Pieces
	public Point[][] pieces = {
			//Red Z
			{new Point(-1,0),new Point(0,0),new Point(0,-1),new Point(1,-1)},
			//Orange L
			{new Point(-1,-1),new Point(-1,0),new Point(0,0),new Point(1,0)},
			//Yellow O
			{new Point(0,0),new Point(1,0),new Point(0,-1),new Point(1,-1)},
			//Green S
			{new Point(1,0),new Point(0,0),new Point(0,-1),new Point(-1,-1)},
			//Cyan I
			{new Point(-1,0),new Point(0,0),new Point(1,0),new Point(2,0)},
			//Blue J
			{new Point(1,-1),new Point(1,0),new Point(0,0),new Point(-1,0)},
			//Purple T
			{new Point(-1,0),new Point(0,0),new Point(0,-1),new Point(1,0)}
		};
	
	//Colours
	public Color[] _colours = {new Color(0,0,0),new Color(230,0,0), new Color(255,128,0), new Color(230,230,50), new Color(0,200,0), new Color(0,230,230), new Color(0,0,230), new Color(150,0,200)};
	Color _borderColour = new Color(128, 128, 128);
	int _borderThickness = 1;
	
	//Distribution of pieces
	int _listLength = 5;
	int _futureSeeing = 2;
	
	//Hold
	int hold = -1;
	
	//Components
	public JLabel[][] board = new JLabel[_boardWidth][_boardHeight];
	public int[][] boardColours = new int[_boardWidth][_boardHeight];
	public JFrame frame;
	public JPanel panel;
	public Timer fall;
	public JLabel[][] nextPiece = new JLabel[_pieceWidth][1 + _pieceHeight * _futureSeeing];
	public JLabel[][] holdShow = new JLabel[_pieceWidth][_pieceHeight + 1];
	
	//UI
	public JButton exit;
	public JButton pause;
	
	//Timer stuffs
	public double _faster = 1.3;
	public Timer remove;
	public int _removeSpeed = 25;
	public int _linesPerLevel = 10;
	
	//Animations
	int line;
	int block;
	
	//The piece
	public piece P = new piece();
	ArrayList<Integer> list = new ArrayList<Integer>();
	public boolean canMove = true;
	
	//Score
	int score = 0;
	int lines = 0;
	int cleared = 0;
	int[] _perPoint = {0,40,100,300,1200};
	JLabel scoreLabel;
	JLabel linesLabel;
	JLabel levelLabel;
	
	//Sounds
	String windowsXPError = "sounds/WindowsXPError.wav";
	String music = "sounds/TetrisMusic.wav";
	public Timer playMusic;
	AudioInputStream inputStream;
	Clip audioLineClip;
	float _skew = (float)Math.exp(5);
	

	public static void main(String[] args){
		new tetris();
	}
	
	public tetris(){
		//Initialising
		Initialise();
	}
	
	public void draw(){		
		//Colours
		for(int i = 0; i < board.length; i++){
			for(int j = 0; j < board[i].length; j++){
				board[i][j].setBackground(_colours[boardColours[i][j]]);
			}
		}
		
		//Window
		for(int i = 0; i < nextPiece.length; i++){
			for(int j = 0; j < nextPiece[i].length; j++){
				nextPiece[i][j].setBackground(_colours[0]);
				Border b = BorderFactory.createMatteBorder(_borderThickness, _borderThickness, _borderThickness, _borderThickness, _borderColour);
				nextPiece[i][j].setBorder(b);
			}
		}
		for(int i = 0; i < _futureSeeing; i++){
			for(int j = 0; j < pieces[list.get(i)].length; j++){
				nextPiece[1 + pieces[list.get(i)][j].x][1 + i * _pieceHeight - pieces[list.get(i)][j].y].setBackground(_colours[list.get(i) + 1]);
				nextPiece[1 + pieces[list.get(i)][j].x][1 + i * _pieceHeight - pieces[list.get(i)][j].y].setBorder(BorderFactory.createBevelBorder(0));
			}
		}
		for(int i = 0; i < holdShow.length; i++){
			for(int j = 0; j < holdShow[i].length; j++){
				holdShow[i][j].setBackground(_colours[0]);
				Border b = BorderFactory.createMatteBorder(_borderThickness, _borderThickness, _borderThickness, _borderThickness, _borderColour);
				holdShow[i][j].setBorder(b);
			}
		}
		if(hold != -1){
			for(int j = 0; j < pieces[hold].length; j++){
				holdShow[1 + pieces[hold][j].x][1 - pieces[hold][j].y].setBackground(_colours[hold + 1]);
				holdShow[1 + pieces[hold][j].x][1 - pieces[hold][j].y].setBorder(BorderFactory.createBevelBorder(0));
			}
		}
	}

	public void Initialise(){
		//Consts
		int _frameWidth = 400;
		int _frameHeight = 400;
		int _blockLength = _frameHeight / (_boardHeight - _boardBuffer);
		int _nextBlockHeight = (int)(0.8 * (double)_blockLength);
		int _nextPieceBufferLeft = 10;
		int _nextPieceBufferUp = 16;
		int _holdShowBufferLeft = 70;
		int _holdShowBufferUp = 16;
		Color _boardColour = _colours[6];
		Dimension _exitButtonSize = new Dimension(70,50);
		Dimension _pauseButtonSize = new Dimension(70,50);
		Point _exitButtonPosition = new Point(_frameWidth - _exitButtonSize.width - 10, _frameHeight - _exitButtonSize.height - 10);
		Point _pauseButtonPosition = new Point( 10, _frameHeight - _pauseButtonSize.height - 10);
		float _defaultMusic = 0.6f;
		Dimension _scoreLabelSize = new Dimension(90,50);
		int _labelBuffer = 10;
		Point _scoreLabelBase = new Point(_labelBuffer, 100);
	
		//New frame
		frame = new JFrame();
		frame.setLocationRelativeTo(null);
		frame.getContentPane().setPreferredSize(new Dimension(_frameWidth, _frameHeight));
		frame.pack();
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){System.exit(0);}});
		frame.setVisible(true);
		panel = new JPanel();
		panel.setLayout(null);
		frame.add(panel);
		
		//UI
		exit = new JButton("Exit?");
		Rectangle _exitBounds = new Rectangle();
		_exitBounds.setSize(_exitButtonSize);
		_exitBounds.setLocation(_exitButtonPosition);
		exit.setBounds(_exitBounds);
		exit.setFocusable(false);
		exit.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
					exit();}
				});
		panel.add(exit);
		pause = new JButton("Pause");
		Rectangle _pauseBounds = new Rectangle();
		_pauseBounds.setSize(_pauseButtonSize);
		_pauseBounds.setLocation(_pauseButtonPosition);
		pause.setBounds(_pauseBounds);
		pause.setFocusable(false);
		pause.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
					pause();}
				});
		panel.add(pause);
		
		//Score Display
		scoreLabel = new JLabel("Score:0");
		linesLabel = new JLabel("Lines:0");
		levelLabel = new JLabel("Level:0");
		scoreLabel.setBounds(_scoreLabelBase.x,_scoreLabelBase.y,_scoreLabelSize.width, _scoreLabelSize.height);
		linesLabel.setBounds(_scoreLabelBase.x,_scoreLabelBase.y + _scoreLabelSize.height,_scoreLabelSize.width, _scoreLabelSize.height);
		levelLabel.setBounds(_scoreLabelBase.x,_scoreLabelBase.y + _scoreLabelSize.height * 2,_scoreLabelSize.width, _scoreLabelSize.height);
		panel.add(scoreLabel);
		panel.add(linesLabel);
		panel.add(levelLabel);
		
		//New board
		for(int i = 0; i < _boardWidth; i++){
			for(int j = 0; j < _boardHeight; j++){
				JLabel l = new JLabel();
				Border b = BorderFactory.createMatteBorder(_borderThickness, _borderThickness, _borderThickness, _borderThickness, _borderColour);
				l.setBorder(b);
				l.setBounds((_frameWidth - _boardWidth * _blockLength) / 2 + i * _blockLength, _frameHeight - (j + 1) * _blockLength, _blockLength, _blockLength);
				l.setOpaque(true);
				l.setBackground(_boardColour);
				board[i][j] = l;
				panel.add(board[i][j]);
			}
		}
		//New next piece
		for(int i = 0; i < nextPiece.length; i++){
			for(int j = 0; j < nextPiece[i].length; j++){
				JLabel l = new JLabel();
				Border b = BorderFactory.createMatteBorder(_borderThickness, _borderThickness, _borderThickness, _borderThickness, _borderColour);
				l.setBorder(b);
				l.setBounds((_frameWidth - _boardWidth * _blockLength) / 2 + _boardWidth * _blockLength + _nextPieceBufferLeft + i * _nextBlockHeight, _nextPieceBufferUp + j * _nextBlockHeight, _nextBlockHeight, _nextBlockHeight);
				l.setOpaque(true);
				l.setBackground(_boardColour);
				nextPiece[i][j] = l;
				panel.add(nextPiece[i][j]);
			}
		}
		//The hold section
		for(int i = 0; i < holdShow.length; i++){
			for(int j = 0; j < holdShow[i].length; j++){
				JLabel l = new JLabel();
				Border b = BorderFactory.createMatteBorder(_borderThickness, _borderThickness, _borderThickness, _borderThickness, _borderColour);
				l.setBorder(b);
				l.setBounds((_frameWidth - _boardWidth * _blockLength) / 2 - _holdShowBufferLeft + i * _nextBlockHeight, _holdShowBufferUp + j * _nextBlockHeight, _nextBlockHeight, _nextBlockHeight);
				l.setOpaque(true);
				l.setBackground(_boardColour);
				holdShow[i][j] = l;
				panel.add(holdShow[i][j]);
			}
		}
		
		frame.addKeyListener(new KeyListener(){
				@Override
				public void keyTyped(KeyEvent e) {}
				@Override
				public void keyPressed(KeyEvent e) {
					//Key press
					keyPress(e);
				}
				@Override
				public void keyReleased(KeyEvent e) {}
				});
		
		//Setting the piece
		restock();
		reset();
		
		//Piece colours
		pieceColour();
		
		//Drawing
		draw();
		
		//Starting
		ActionListener f = new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					fall();
				}
			};
		fall = new Timer(_fallTime,f);
		fall.start();
		
		//Other timer
		ActionListener r = new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					removeBlock();
				}
			};
		remove = new Timer(_removeSpeed,r);
		
		//Playing the music
		try {
			inputStream = AudioSystem.getAudioInputStream(new File(music));
	
			audioLineClip = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));
			audioLineClip.open(inputStream);
			FloatControl control = (FloatControl) audioLineClip.getControl(FloatControl.Type.MASTER_GAIN);
			float range = control.getMaximum() - control.getMinimum();
			float gain = (range * skew(_defaultMusic)) + control.getMinimum();
			control.setValue(gain);
			audioLineClip.setFramePosition(0);
			audioLineClip.loop(Clip.LOOP_CONTINUOUSLY);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	public void keyPress(KeyEvent e){
		//Can move?
		if(canMove){
			//Blacking out
			blackOut();
			
			//Rotating (UP key)
			if(e.getKeyCode() == 38){
				if(e.getModifiersEx() == 0){
					pieceRotate();
				}else{
					pieceRotateBackwards();
				}
			//Down (DOWN key)
			}else if(e.getKeyCode() == 40){
				//No dropping
				fall.stop();
				fall.start();
				
				//Going down
				if(pieceDown()){
					fall();
				}
			//Left (LEFT key)
			}else if(e.getKeyCode() == 37){
				pieceLeft();
			//Right (RIGHT key)
			}else if(e.getKeyCode() == 39){
				pieceRight();
			//Drop (SPACE key)
			}else if(e.getKeyCode() == 32){
				while(!pieceDown()){}
				fall();
			//Hold ('f' key)
			}else if(e.getKeyCode() == 70){
				hold();
			}
			
			//Piece Colours
			pieceColour();
		}
		
		//Drawing
		draw();
	}
	
	public void fall(){
		//Blacking out
		blackOut();
		
		//Move and reset
		if(pieceDown()){
			pieceColour();
		
			if(fullRow() >= 0){
				cleared = 0;
				fall.stop();
				canMove = false;
				line = fullRow();
				lineRemove();
				return;
			}
			
			//New piece
			reset();
			
			//Losing
			for(int i = 0; i < P.points.length; i++){
				if(boardColours[P.location.x + P.points[i].x][P.location.y + P.points[i].y] != 0){
					lose();
				}
			}
		}
		
		//Piece colours
		pieceColour();
		
		//Drawing
		draw();
	}
	
	public void pieceRotate(){
		//Rotating
		piece Q = P.rotate();
		for(int i = 0; i < Q.points.length; i++){
			//Bounds?
			int y = Q.location.y + Q.points[i].y;
			int x = Q.location.x + Q.points[i].x;
			if(!(y >= 0 && y < _boardHeight && x >= 0 && x < _boardWidth)){
				playSound(windowsXPError);
				return;
			}else if(boardColours[x][y] != 0){
				playSound(windowsXPError);
				return;
			}
		}
		P = Q;
	}
	
	public void pieceRotateBackwards(){
		//Rotating
		piece Q = P.rotate().rotate().rotate();
		for(int i = 0; i < Q.points.length; i++){
			//Bounds?
			int y = Q.location.y + Q.points[i].y;
			int x = Q.location.x + Q.points[i].x;
			if(!(y >= 0 && y < _boardHeight && x >= 0 && x < _boardWidth)){
				playSound(windowsXPError);
				return;
			}else if(boardColours[x][y] != 0){
				playSound(windowsXPError);
				return;
			}
		}
		P = Q;
	}
	
	public boolean pieceDown(){
		//Blacking out
		
		//Moving
		for(int i = 0; i < P.points.length; i++){
			//Bounds?
			int y = P.location.y + P.points[i].y - 1;
			int x = P.location.x + P.points[i].x;
			if(!(y >= 0 && y < _boardHeight)){
				return true;
			}else if(boardColours[x][y] != 0){
				return true;
			}
		}
		//Down
		P.location.y --;

		return false;
	}
	
	public boolean pieceRight(){
		//Moving
		for(int i = 0; i < P.points.length; i++){
			//Bounds?
			int y = P.location.y + P.points[i].y;
			int x = P.location.x + P.points[i].x + 1;
			if(!(x >= 0 && x < _boardWidth)){
				return true;
			}else if(boardColours[x][y] != 0){
				return true;
			}
		}
		//Down
		P.location.x ++;
		
		return false;
	}
	
	public boolean pieceLeft(){
		//Moving
		for(int i = 0; i < P.points.length; i++){
			//Bounds?
			int y = P.location.y + P.points[i].y;
			int x = P.location.x + P.points[i].x - 1;
			if(!(x >= 0 && x < _boardWidth)){
				return true;
			}else if(boardColours[x][y] != 0){
				return true;
			}
		}
		//Down
		P.location.x --;
		
		return false;
	}
	
	public void lose(){
		pausing();
		frame.dispose();
		JOptionPane.showMessageDialog(null, "Sorry, you lost. You got a score of " + score + ". And cleared " + lines + " lines.");
	}
	
	public void lineRemove(){	
		//Starting the timers
		block = 0;
		remove.start();
		
		//Score and lines
		lines ++;
		linesLabel.setText("Lines:" + lines);
		cleared ++;
		//Faster
		if(Math.IEEEremainder(lines, _linesPerLevel) == 0){
			fall.setDelay((int)((double)fall.getDelay() / _faster));
			levelLabel.setText("Level:" + (int)(lines / _linesPerLevel));
		}
	}
	
	public void removeBlock(){
		//End loop
		if(block == _boardWidth){
			//Stopping
			remove.stop();
			
			//Redoing the lines
			for(int j = line; j < _boardHeight - 2; j++){
				for(int k = 0; k < _boardWidth; k++){
					boardColours[k][j] = boardColours[k][j+1];
					board[k][j].setBorder(board[k][j+1].getBorder()); 
				}
			}
			
			//Drawing
			draw();
			
			//Next line
			if(fullRow() >= 0){
				line = fullRow();
				lineRemove();
			}else{
				//Restarting
				fall.start();
				canMove = true;
				reset();
				
				//Drawing
				pieceColour();
				draw();
				
				//Falling
				fall();
				
				//Score
				score += _perPoint[cleared] * ((int)Math.floor((double)lines / (double)_linesPerLevel) + 1);
				scoreLabel.setText("Score:" + score);
			}
		
			//Stop
			return;
		}
		
		//Removing the block
		Border b = BorderFactory.createMatteBorder(_borderThickness, _borderThickness, _borderThickness, _borderThickness, _borderColour);
		board[block][line].setBorder(b);
		boardColours[block][line] = 0;
		
		//Drawing
		draw();
		
		//Looping
		block++;
	}
	
	public int fullRow(){
		//Row clearing
		for(int i = _boardHeight - 1; i >= 0; i--){
			boolean flag = true;
			for(int j = 0; j < _boardWidth; j++){
				if(boardColours[j][i] == 0){
					flag = false;
					break;
				}
			}
			
			//Clear line
			if(flag){
				return i;
			}
		}
		
		return -1;
	}
	
	public void blackOut(){
		//Blacking out
		for(int i = 0; i < P.points.length; i++){
			boardColours[P.location.x + P.points[i].x][P.location.y + P.points[i].y] = 0;
			Border b = BorderFactory.createMatteBorder(_borderThickness, _borderThickness, _borderThickness, _borderThickness, _borderColour);
			board[P.location.x + P.points[i].x][P.location.y + P.points[i].y].setBorder(b);
		}
	}
	
	public void pieceColour(){
		//Piece colours
		for(int i = 0; i < P.points.length; i++){
			boardColours[P.location.x + P.points[i].x][P.location.y + P.points[i].y] = P.type + 1;
			//Border stuff
			ArrayList<Point> points = new ArrayList<>(Arrays.asList(P.points));
			int[] borderDefault = new int[4];
			int[] borderEdge = new int[4];
			//Right
			if(points.contains(new Point(P.points[i].x + 1, P.points[i].y))){
				borderDefault[3] = _borderThickness;
			}else{
				borderEdge[3] = _borderThickness;
			}
			//Left
			if(points.contains(new Point(P.points[i].x - 1, P.points[i].y))){
				borderDefault[1] = _borderThickness;
			}else{
				borderEdge[1] = _borderThickness;
			}
			//Up
			if(points.contains(new Point(P.points[i].x, P.points[i].y + 1))){
				borderDefault[0] = _borderThickness;
			}else{
				borderEdge[0] = _borderThickness;
			}
			//Down
			if(points.contains(new Point(P.points[i].x, P.points[i].y - 1))){
				borderDefault[2] = _borderThickness;
			}else{
				borderEdge[2] = _borderThickness;
			}
			Border b = BorderFactory.createBevelBorder(0);//Default bevel border
			board[P.location.x + P.points[i].x][P.location.y + P.points[i].y].setBorder(b);
		}
	}
	
	public void restock(){
		//Shuffle
		
		//New
		@SuppressWarnings("unchecked")
		ArrayList<Integer> stub = (ArrayList<Integer>) list.clone();
		list.clear();
		
		//Base 
		for(int i = 0; i < _listLength; i++){
			for(int j = 0; j < _colours.length - 1; j++){
				list.add(j);
			}
		}
		
		//Random non-doubling list
		@SuppressWarnings("unchecked")
		ArrayList<Integer> q = (ArrayList<Integer>) list.clone();
		list.clear();
		
		//Seed
		list.add(q.get(0));
		q.remove(0);
		
		while(q.size() > 0){
			//Valid placements
			ArrayList<Integer> spots = new ArrayList<Integer>();
			
			//End
			if(list.get(0) != q.get(0)){
				spots.add(0);
			}
			//Middle
			for(int i = 1; i < list.size() - 1; i++){
				if(list.get(i) != q.get(0) && list.get(i-1) != q.get(0)){
					spots.add(i);
				}
			}
			
			//Choose
			int i = (int) Math.floor(Math.random() * (double)spots.size());
			list.add(spots.get(i), q.get(0));
			q.remove(0);
		}
		
		//No doubles ever
		if(stub.size() > 0 && list.get(0) == stub.get(stub.size() - 1)){
			list.clear();
			list = stub;
			restock();
			return;
		}else{
			list = append(stub, list);
		}
	}
	
	public ArrayList<Integer> append(ArrayList<Integer> a, ArrayList<Integer> b){
		//Adding
		for(int i = 0; i < b.size(); i++){
			a.add(b.get(i));
		}
		//Out
		return a;
	}
	
	public void reset(){
		//Resetting the piece
		P.location = new Point(_boardWidth / 2, _boardHeight - _boardBuffer - 1);
		P.setType(nextType());
		if(list.size() < _futureSeeing){
			restock();
		}
	}
	
	public int nextType(){		
		int i = list.get(0);
		list.remove(0);
		return i;
	}
	
	public class piece{
		//Consts
		int yellow = 2;
		int cyan = 4;
		
		//Vars
		Point[] points = new Point[4];
		int type;
		Point location = new Point();
		
		public void setType(int t){
			type = t;
			points = pieces[t];
		}
		
		public void setPoints(Point[] p){
			points = p;
		}
	
		//Rotate
		public piece rotate(){
			//If yellow
			if(type == yellow){
				return this;
			}else if(type == cyan){
				Point[] p = new Point[4];
				for(int i = 0; i < points.length; i++){
					//Rotating
					p[i] = new Point(points[i].y, -points[i].x);
				}
				
				piece r = new piece();
				r.setType(type);
				r.location = location;
				r.location.x += points[3].x / 2;
				r.location.y += points[3].y / 2;
				r.setPoints(p);
				return r;
			}else{
				Point[] p = new Point[4];
				for(int i = 0; i < points.length; i++){
					//Rotating
					p[i] = new Point(points[i].y,-points[i].x);
				}
				
				piece r = new piece();
				r.setType(type);
				r.location = location;
				r.setPoints(p);
				return r;
			}
		}
	}
	
	public void hold(){
		//Replacing
		int temp = hold;
		hold = P.type;
		if(temp != -1){
			for(int i = 0; i < pieces[temp].length; i++){
				if(P.location.x + pieces[temp][i].x >= _boardWidth || P.location.x + pieces[temp][i].x < 0 || P.location.y + pieces[temp][i].y < 0){
					hold = temp;
					playSound(windowsXPError);
					return;
				}
			}
			P.setType(temp);
		}else{
			reset();
		}
	}
	
	public void playSound(String t){
		try {
			// Open an audio input stream.
			URL url = new File(t).toURI().toURL();
			if(url == null){
				System.out.println("Failed to play sound " + t + ".");
				return;
			}
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
			// Get a sound clip resource.
			Clip clip = AudioSystem.getClip();
			// Open audio clip and load samples from the audio input stream.
			clip.open(audioIn);
			//Changing the volume
			FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			FloatControl control = (FloatControl) audioLineClip.getControl(FloatControl.Type.MASTER_GAIN);
			c.setValue(control.getValue());
			//Starting
			clip.start();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	public void exit(){
		//Pausing
		pausing();
		
		//exit.
		
		//Exiting
		if(JOptionPane.showConfirmDialog(exit, "Do you want to exit?") == 0){
			frame.dispose();
		}else{
			unpause();
		}
	}
	
	public void pausing(){
		//Stopping all
		fall.stop();
		audioLineClip.stop();
		if(remove.isRunning()){
			remove.stop();
			//Redoing the lines
			for(int j = line; j < _boardHeight - 2; j++){
				for(int k = 0; k < _boardWidth; k++){
					boardColours[k][j] = boardColours[k][j+1];
					board[k][j].setBorder(board[k][j+1].getBorder()); 
				}
			}
			lines++;
			//Faster
			if(Math.IEEEremainder(lines, _linesPerLevel) == 0){
				fall.setDelay((int)((double)fall.getDelay() / _faster));
				levelLabel.setText("Level:" + (int)(lines / _linesPerLevel));
			}
			//Removing all of the other lines
			while(fullRow() != -1){
				cleared++;
				lines++;
				//Faster
				if(Math.IEEEremainder(lines, _linesPerLevel) == 0){
					fall.setDelay((int)((double)fall.getDelay() / _faster));
					levelLabel.setText("Level:" + (int)(lines / _linesPerLevel));
				}
				line = fullRow();
				//Redoing the lines
				for(int j = line; j < _boardHeight - 2; j++){
					for(int k = 0; k < _boardWidth; k++){
						boardColours[k][j] = boardColours[k][j+1];
						board[k][j].setBorder(board[k][j+1].getBorder()); 
					}
				}
			}
			
			//Restarting
			canMove = true;
			reset();
			
			//Drawing
			pieceColour();
			draw();
			
			//Score
			score += _perPoint[cleared] * ((int)Math.floor((double)lines / (double)_linesPerLevel) + 1);
			scoreLabel.setText("Score:" + score);
		}
		
		//No button presses
		for(var b: panel.getComponents()){
			if(b.getClass() == exit.getClass()){
				b.setEnabled(false);
			}
		}
	}
	
	public void unpause(){
		//Unpausing
		fall();
		fall.start();
		audioLineClip.loop(Clip.LOOP_CONTINUOUSLY);
		
		//Yes button presses
		for(var b: panel.getComponents()){
			if(b.getClass() == exit.getClass()){
				b.setEnabled(true);
			}
		}
	}
	
	public void pause(){
		//Pausing
		pausing();
		
		//Slider Constants
		int _pauseFrameWidth = 200;
		int _pauseFrameHeight = 400;
		int _sliderWidth = (int)(_pauseFrameWidth * 0.8);
		int _sliderHeight = 50;
		int _sliderLabelWidth = 100;
		int _sliderLabelHeight = 50;
		Dimension _sliderDimension = new Dimension(_sliderWidth,_sliderHeight);
		Point _sliderLocation = new Point((int)(_pauseFrameWidth * 0.1),_sliderHeight);
		Point _sliderLabelLocation = new Point((_pauseFrameWidth - _sliderLabelWidth) / 2, 10);
		Rectangle _sliderBounds = new Rectangle();
		_sliderBounds.setSize(_sliderDimension);
		_sliderBounds.setLocation(_sliderLocation);
		
		//Button Constants
		int _exitResumeWidth = 120;
		int _exitResumeHeight = 50;
		int _buttonBuffer = 10;
		
		//Opening the pause frame
		JFrame pauseFrame = new JFrame("Paused");
		pauseFrame.setLocationRelativeTo(null);
		pauseFrame.getContentPane().setPreferredSize(new Dimension(_pauseFrameWidth, _pauseFrameHeight));
		pauseFrame.pack();
		pauseFrame.setResizable(false);
		pauseFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pauseFrame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){unpause();}});
		pauseFrame.setVisible(true);
		JPanel pausePanel = new JPanel();
		pausePanel.setLayout(null);
		pauseFrame.add(pausePanel);
		
		//Volume slider
		JLabel sliderLabel = new JLabel("Volume");
		sliderLabel.setHorizontalAlignment(SwingConstants.CENTER);
		sliderLabel.setLocation(_sliderLabelLocation);
		sliderLabel.setBounds((_pauseFrameWidth - _sliderLabelWidth) / 2, 10, _sliderLabelWidth, _sliderLabelHeight);
		pausePanel.add(sliderLabel);
		JSlider slider = new JSlider();
		slider.setBounds(_sliderBounds);
		FloatControl control = (FloatControl) audioLineClip.getControl(FloatControl.Type.MASTER_GAIN);
		float range = control.getMaximum() - control.getMinimum();
		float val = ((control.getValue() - control.getMinimum())) / range;
		slider.setValue((int)(100 * unskew(val)));
		pausePanel.add(slider);
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ce) {
				//Setting the sounds
				float gain = range * skew(((JSlider)ce.getSource()).getValue() / 100f) + control.getMinimum();
				control.setValue(gain);
			}
		});
		
		//Exit? or Resume? or Restart?
		JButton pauseExit = new JButton("Exit?");
		JButton pauseResume = new JButton("Resume");
		JButton restartGame = new JButton("Restart");
		pauseExit.setBounds((_pauseFrameWidth - _exitResumeWidth) / 2, _pauseFrameHeight - _exitResumeHeight - _buttonBuffer, _exitResumeWidth, _exitResumeHeight);
		pauseResume.setBounds((_pauseFrameWidth - _exitResumeWidth) / 2, _pauseFrameHeight - _exitResumeHeight * 2 - _buttonBuffer * 2, _exitResumeWidth, _exitResumeHeight);
		restartGame.setBounds((_pauseFrameWidth - _exitResumeWidth) / 2, _pauseFrameHeight - _exitResumeHeight * 3 - _buttonBuffer * 3, _exitResumeWidth, _exitResumeHeight);
		pauseResume.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
					pauseFrame.dispose(); unpause();}
				});
		pauseExit.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
					frame.dispose();}
				});
		restartGame.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
			frame.dispose();
			pauseFrame.dispose();
			new tetris();
			}
		});
		pausePanel.add(pauseExit);
		pausePanel.add(pauseResume);
		pausePanel.add(restartGame);
   }
	
	public float skew(float f){
		return (float)(Math.log((_skew-1) * f + 1)/Math.log(_skew));
	}
	
	public float unskew(float f){
		return (float)(Math.pow(_skew, f) - 1)/(_skew - 1);
	}
}