package net.quasarsolutions.penlet.games;

import com.livescribe.penlet.Penlet;
import com.livescribe.afp.PageInstance;
import com.livescribe.afp.PropertyCollection;
import com.livescribe.display.BrowseList;
import com.livescribe.display.Display;
import com.livescribe.event.HWRListener;
import com.livescribe.event.StrokeListener;
import com.livescribe.icr.ICRContext;
import com.livescribe.icr.Resource;
import com.livescribe.penlet.PenletStateChangeException;
import com.livescribe.penlet.Region;
import com.livescribe.ui.ScrollLabel;
import com.livescribe.event.MenuEventListener;
import com.livescribe.event.MenuEvent;
import com.livescribe.ui.MediaPlayer;
import com.livescribe.penlet.RegionCollection;
import java.util.Vector;
import java.util.Random;
import java.lang.Integer;
import java.lang.Character;
import java.io.IOException;
import java.io.InputStream;
import com.livescribe.event.PenTipListener;
import com.livescribe.storage.StrokeStorage;
import com.livescribe.geom.Rectangle;
import com.livescribe.configuration.Config;
import com.livescribe.display.Image;
import net.quasarsolutions.penlet.utils.MenuBrowseListItem;

/**
 * This class uses intelligent character recognition (ICR) to detect characters specified
 * by the user in the ICR wizard
 */
public class CodeBreaker extends Penlet implements StrokeListener, HWRListener, MenuEventListener, PenTipListener {

	// penlet variables
	private Display display;
    private ScrollLabel label;
    private ICRContext icrContext;
    private Config ConfigData;
    private PropertyCollection props;
	MediaPlayer player;
    
    //menu variables
    private Vector vectorMenuItems;
    private MenuBrowseListItem[] myMenuItems;
    private BrowseList menuBrowseList;
    private String[] menusounds;
    int currentMenuSelection,menuSoundPos,menuGameModePos,menuHelpPos;
    boolean[] menuItemIsSelectable;
    boolean menuHelpOnDisplay;
   
    //options
    boolean soundon;
    long gamemode;
    long gameModeBasic=1,gameModeInter=2,gameModeAdvanced=3;
 
    //game states
    int gamestate=0,initializing=1,initapp=2, actapp=3, menuoptions=4,startgameboard=5;
    int makegameboard=6,gameinprogress=7, gamepaused=8, gameover=9,deactapp=10;
    
    // game variables 
    
    private String codetocrack;
    private String[] guess, result;
    int guesscount,guesstodisplay,numberofxcoordinates;
    int[] codedigitcount,xcoordinates;

    //gameboard regions
    long guessRegion,clearRegion,soundRegion,newGameRegion,singletaptime;
    Rectangle RegionRect;
    boolean makingRegionRect;
    
    //keeping track if regions have been defined i.e. gameboard has been created
    
    boolean guessRegionDefined, clearRegionDefined, soundRegionDefined, newGameRegionDefined, gameBoardReady;

    // miscellaneous constants
    
    String abortCode;
    long soundpropid=1,gamemodepropid=2;
    int maxGuesses=10;
    
    //image streams
    InputStream inputstreamImage;
    
    //images
    
    Image imageBull, imageCow, imageWrong,imageHelp,imageAdvanced,imageBasic,imageInter;
    Image imageGameBoard,imageClearButton,imageNewGameButton,imageSoundButton;
    /** Penlet Constructor
     * 
     */
    public CodeBreaker(){


        
    	// gamestate
    	
    	gamestate=initializing;
    	
    	// initialize the guess and result arrays. 
        guess=new String[maxGuesses];
    	result=new String[maxGuesses];
    	codedigitcount=new int[10];
    	
        //initialize region information
    	
    	guessRegion=1;
        clearRegion=2;
        soundRegion=3;
        newGameRegion=4;
        guessRegionDefined=clearRegionDefined=soundRegionDefined=newGameRegionDefined=gameBoardReady=false;
        RegionRect=new Rectangle();
        makingRegionRect=false;

        //misc variables
        singletaptime=0;
        xcoordinates=new int[4];
        numberofxcoordinates=0;
        abortCode="0000";
        
    }
	private void makeMenu() {
		// add items to the menu
        this.vectorMenuItems=new Vector();
        this.myMenuItems=new MenuBrowseListItem[5];
        
        myMenuItems[0]=new MenuBrowseListItem(this.logger,true,null,null,ConfigData.getStringValue("menuNewGame"));
        myMenuItems[1]=new MenuBrowseListItem(this.logger,true,null,null,ConfigData.getStringValue("menuSound"));
        myMenuItems[2]=new MenuBrowseListItem(this.logger,true,null,null,ConfigData.getStringValue("menuGameMode"));
        myMenuItems[3]=new MenuBrowseListItem(this.logger,true,null,null,ConfigData.getStringValue("menuHelp"));
        myMenuItems[4]=new MenuBrowseListItem(this.logger,false,null,null,ConfigData.getStringValue("menuAbout"));


        for (int i=0;i<5;i++) {
        	this.vectorMenuItems.addElement(myMenuItems[i]);
        }

    	// position of sound & game mode menu items
        
        menuSoundPos=1;
        menuGameModePos=2;
        menuHelpPos=3;
        menuHelpOnDisplay=false;

        // add sounds for the menu items
        menusounds=new String[5];
        menusounds[0]="newgame";
        menusounds[1]="sound";
        menusounds[2]="gamemode";
        menusounds[3]="help";
        menusounds[4]="";
   
        //indicate menu items that can be selected
        menuItemIsSelectable=new boolean[5];
        menuItemIsSelectable[0]=true;
        menuItemIsSelectable[1]=true;
        menuItemIsSelectable[2]=true;
        menuItemIsSelectable[3]=true;
        menuItemIsSelectable[4]=false;
	}
    /**
     * Called when the penlet is created
     */
	public void initApp() throws PenletStateChangeException {
		
		this.player = MediaPlayer.newInstance(this);
        this.display = this.context.getDisplay();
        this.label = new ScrollLabel();
        
        
        //remove the old property file if it exists
        if (PropertyCollection.exists(this.context,"BreakTheCodeProperties")){
            props=PropertyCollection.getInstance(this.context, "BreakTheCodeProperties",false);
            props.destroy();      	
        }

        //get the new property file. create if it does not exist
        props=PropertyCollection.getInstance(this.context, "CodeBreakerProperties", true);


    	//read the config data
    	this.ConfigData=this.context.getAppConfiguration();
    	soundon=ConfigData.getBooleanValue("Sound");
        gamemode=ConfigData.getLongValue("GameMode");

        makeMenu();
    	
        // gamestate
    	gamestate=initapp;
    	
        
        //read the property data
        soundon=readBooleanProperty(soundpropid);
        updateSoundMenu();

        gamemode=readIntegerProperty(gamemodepropid);
        if (gamemode==0){
        	gamemode=gameModeBasic;
        }
        updateGameModeMenu();
	}

	private void updateSoundMenu() {
		
		this.vectorMenuItems.removeElementAt(menuSoundPos);
		if (soundon){
			myMenuItems[menuSoundPos].updateTitle("Sound:ON");
	        menusounds[menuSoundPos]="soundon";
			
		} else {
			myMenuItems[menuSoundPos].updateTitle("Sound:OFF");
	        menusounds[menuSoundPos]="soundoff";
		}
        this.vectorMenuItems.insertElementAt(myMenuItems[menuSoundPos], menuSoundPos);
	}
	
	private void updateGameModeMenu() {
		
		this.vectorMenuItems.removeElementAt(menuGameModePos);

		if (gamemode==gameModeBasic){
			myMenuItems[menuGameModePos].updateTitle("Mode:Basic");
	        menusounds[menuGameModePos]="basicgamemode";
		} else if (gamemode==gameModeInter){
			myMenuItems[menuGameModePos].updateTitle("Mode:Intermediate");
	        menusounds[menuGameModePos]="intermediategamemode";
		} else if (gamemode==gameModeAdvanced){
			myMenuItems[menuGameModePos].updateTitle("Mode:Advanced");
	        menusounds[menuGameModePos]="advancedgamemode";
		}
        this.vectorMenuItems.insertElementAt(myMenuItems[menuGameModePos], menuGameModePos);
	}
	
	private boolean readBooleanProperty(long propid) {

		String propertyvalue;
		propertyvalue= props.getProperty(propid);

		if (propertyvalue!=null){
			if (propertyvalue.equals("off"))
			{
				return false;
			}
		}
	    return true;
	}
		
	private long readIntegerProperty(long propid) {

		String propertyvalue;
		long returnvalue=0;
		
		propertyvalue= props.getProperty(propid);
		if (propertyvalue!=null)
		{
			try {

				returnvalue=Long.parseLong(propertyvalue);

			} catch (NumberFormatException e){

				returnvalue=0;			

			}
		}
		
		return returnvalue;
	}

	/**
     * Called when the penlet is activated by menu
     */
	public void activateApp(int reason, Object[] args) {
        // gamestate
    	gamestate=actapp;

    	//add listeners
		this.context.addStrokeListener(this);
		context.addPenTipListener(this);
		
        // Configure the ICR context
        this.icrContext = this.context.getICRContext(1000, this);
        Resource[] resources = {
        	this.icrContext.getDefaultAlphabetKnowledgeResource(),
			//this.icrContext.createLKSystemResource(ICRContext.SYSRES_LK_WORDLIST_100K),
			this.icrContext.createSKSystemResource(ICRContext.SYSRES_SK_DIGIT)
        };
        this.icrContext.addResourceSet(resources);
        
        //create the menu
        this.menuBrowseList = new BrowseList(this.vectorMenuItems,null);

		//load image streams
        Class myPenLetClass=this.getClass();
        inputstreamImage=myPenLetClass.getResourceAsStream("/images/Bull.arw");
        
        
        try {
			imageBull=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageBull=null;
		}

        inputstreamImage=myPenLetClass.getResourceAsStream("/images/Cow.arw");
		try {
			imageCow=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageCow=null;
		}

		inputstreamImage=myPenLetClass.getResourceAsStream("/images/Wrong.arw");
        try {
			imageWrong=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageWrong=null;
		}
    
        inputstreamImage=myPenLetClass.getResourceAsStream("/images/Help.arw");
		try {
			imageHelp=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageWrong=null;
		}

        inputstreamImage=myPenLetClass.getResourceAsStream("/images/AdvancedMode.arw");
		try {
			imageAdvanced=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageAdvanced=null;
		}

		inputstreamImage=myPenLetClass.getResourceAsStream("/images/InterMediateMode.arw");
		try {
			imageInter=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageInter=null;
		}
		
        inputstreamImage=myPenLetClass.getResourceAsStream("/images/BasicMode.arw");
		try {
			imageBasic=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageBasic=null;
		}

        inputstreamImage=myPenLetClass.getResourceAsStream("/images/GBGuesses.arw");
		try {
			imageGameBoard=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageGameBoard=null;
		}

        inputstreamImage=myPenLetClass.getResourceAsStream("/images/GBClear.arw");
		try {
			imageClearButton=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageClearButton=null;
		}

        inputstreamImage=myPenLetClass.getResourceAsStream("/images/GBSound.arw");
		try {
			imageSoundButton=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageSoundButton=null;
		}

        inputstreamImage=myPenLetClass.getResourceAsStream("/images/GBNewGame.arw");
		try {
			imageNewGameButton=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			imageNewGameButton=null;
		}

		if (reason==ACTIVATED_BY_MENU) {
			
        	//display the first menu item
            this.display.setCurrent(this.menuBrowseList);
            currentMenuSelection=this.menuBrowseList.getFocusIndex();
            playMenuPrompt(currentMenuSelection);
        } else {
        	/* activated by another event. state that codebreaker active
        	   and launch into new game */
        	String[] filenames;
        	filenames=new String[3];
        	filenames[0]="codebreaker";
        	filenames[1]="newgame";
        	filenames[2]="rectangle";
        	playSound(filenames);
        	gamestate=newGame(false);
        }
        
	}

    /**
     * Called when the penlet is deactivated
     */
	public void deactivateApp(int reason) {

		//remove listeners
		this.context.removeStrokeListener(this);
		context.removePenTipListener(this);
        icrContext.dispose();
        icrContext = null;
        
        gamestate=deactapp;
       
        //update the properties
        updateBoolProperty(soundpropid,soundon);
        updateIntegerProperty(gamemodepropid,gamemode);
        
        //log the reason
 	}
	
	private void updateBoolProperty(long propid, boolean value) {
		if (value)
        {
        	props.setProperty(propid, "on", true);
        } 
        else
        {
        	props.setProperty(propid, "off", true);
        	
        }
	}

	private void updateIntegerProperty(long propid, long value) {

		props.setProperty(propid, String.valueOf(value), true);
	}

	/**
     * Called when the penlet is deallocated
     */
	public void destroyApp() throws PenletStateChangeException {}

    /**
     * Called when a new stroke is created on the pen. 
     * The stroke information is added to the ICRContext
     */
	
	public void strokeCreated(long time, Region areaID, PageInstance page) {
		/* the icr engine is only needed when a game is in progress
		 * and the game board has been completely drawn
		 */

		//only process if the stroke is not part of a single tap
		if (time>singletaptime){
			if (gamestate==gameinprogress && gameBoardReady){
				// only interested in recognizing if it is written in the guess region
				if (areaID.getAreaId()==guessRegion){
					// check if game is over
					if (gamestate==gameinprogress){
						this.icrContext.addStroke(page, time);
						StrokeStorage ss=new StrokeStorage(page);
						Rectangle rect=ss.getStrokeBoundingBox(time);
						if (numberofxcoordinates < 4)
						{ xcoordinates[numberofxcoordinates]=rect.getX();
						  numberofxcoordinates++;
						}
					}
				}
			} else {
				if (gamestate==makegameboard || gamestate==startgameboard){
					int areaid;
					
					areaid=areaID.getAreaId();
					gamestate=makegameboard;
					
					if (areaid!=guessRegion && areaid !=clearRegion && areaid !=soundRegion && areaid !=newGameRegion){
						RegionCollection rc=this.context.getCurrentRegionCollection();
						StrokeStorage ss=new StrokeStorage(page);
						Rectangle rect=ss.getStrokeBoundingBox(time);
						if (makingRegionRect){
							RegionRect=rect;
							makingRegionRect=false;
						} else {
							RegionRect=Rectangle.getUnion(RegionRect,rect);
						}
						if (rc.isOverlappingExistingRegion(RegionRect)){
							// overlaps existing region, beep and do not do anything
							playSound("SL_Error");
						}
					}
				}
			}
		}
		
		//reset the single tap time
		singletaptime=0;
		
	}

    /**
     * When the user pauses (pause time specified by the wizard),
     * all strokes in the ICRContext are cleared
     */
	public void hwrUserPause(long time, String result) {
		
		//only process when there is a code to crack
		if (gamestate==gameinprogress){

			//remove any spaces from the string recognized by the pen
			result=result.trim();
			String newresult="";
			for (int i=0;i<result.length();i++){
				if (result.charAt(i)!=' '){
					newresult+=result.charAt(i);
				}
			}
			
			//add the result to the one existing
			guess[guesscount-1]+=newresult;
			int guesslength=guess[guesscount-1].length();
			
			/* now sort based on x coordinates of each digits. 
			 * split into array of char, then do bubble sort in each char
			 * once complete, reassemble the data to the represent
			 * the value written by the user
			 */
			
			char[] tempresultc;
			
			tempresultc=new char[guesslength];
			
			//extract the individual characters from the guess
			for (int i=0;i<guesslength;i++){
				tempresultc[i]=guess[guesscount-1].charAt(i);
			}
			
			/* bubble sort all the individual digits based on the xcoordinates
			 * stored during the stroke creation process 
			 */
			int tempx;
			char tempc;
			for (int i=0;i< numberofxcoordinates;i++)
			{
				for (int j=0;j < numberofxcoordinates-1;j++){
					if (xcoordinates[j]>xcoordinates[j+1]){

						//swap the coordinates
						tempx=xcoordinates[j];
						xcoordinates[j]=xcoordinates[j+1];
						xcoordinates[j+1]=tempx;

						//swap the digits
						tempc=tempresultc[j];
						tempresultc[j]= tempresultc[j+1];
						tempresultc[j+1]=tempc;
					}
				}
			}

			//now add tempresult back in the correct order
			guess[guesscount-1]="";
			for (int i=0;i<guesslength;i++){
				guess[guesscount-1]+=tempresultc[i];
			}
			
			guesstodisplay=guesscount;

			int length=guess[guesscount-1].length();

			if (length==4){
				/* we have enough digits. if abortCode then play code and game over
				otherwise compare.  */
				
				numberofxcoordinates=0;
				
				if (guess[guesscount-1].compareTo(abortCode)!=0){

					int[] guessdigitcount;
					String [] tempresult;
				
					guessdigitcount=new int[10];
					tempresult=new String[4];
	
					for (int j=0;j<10;j++){
						guessdigitcount[j]=0;
					}
	
					//initialize the temp result 
					for (int j=0;j<4;j++){
						tempresult[j]="-";
					}
	
					// go through all the guess and find the blacks
									
					for (int i=0;i<4;i++){
						char digit=guess[guesscount-1].charAt(i);
						int intvalueofdigit;			
						if (digit==codetocrack.charAt(i)){
							intvalueofdigit=Character.digit(digit,10);
							guessdigitcount[intvalueofdigit]++;
							tempresult[i]="b";
						}
					}
					
					// now for any place that does not have a black, check for white only if count not exceeded
					for (int i=0;i<4;i++){
						boolean matchfound=false;
						char digit=guess[guesscount-1].charAt(i);
						int intvalueofdigit=Character.digit(digit,10);
						
						if (tempresult[i]=="-"){
							if (guessdigitcount[intvalueofdigit]< codedigitcount[intvalueofdigit]){
								guessdigitcount[intvalueofdigit]++;
								for (int j=0;j<4 && !matchfound;j++){
									char code=codetocrack.charAt(j);
									if (i!=j){
										if (digit==code){
											tempresult[i]="w";
											matchfound=true;
										}
									}
								}
							}
						}
						this.result[guesscount-1]+=tempresult[i];
					}
					
					/* if we are in advanced game mode, then do not reveal the
					 * position of the bulls & cows. just give the total count
					 * in the format <n>b<m>w. example 3b1w or 3b0w 
					*/ 
					
					if (gamemode==gameModeInter || gamemode==gameModeAdvanced){
						int bcount=0,wcount=0;
						
						for(int i=0;i<4;i++){
							String currchar=this.result[guesscount-1].substring(i,i+1);
							
							if (currchar.equals("w")){
								wcount++;
							} else if (currchar.equals("b")){
								bcount++;
							}
						}
						this.result[guesscount-1]=String.valueOf(bcount)+"b"+String.valueOf(wcount)+"w";
					}
	
					if (this.result[guesscount-1].equals("bbbb") || (this.result[guesscount-1].equals("4b0w"))){
						codeBroken();
					} else if (guesscount!=maxGuesses){
						promptForGuess(guesscount);
						guesscount++;
					} else {
							//all ten attempts exhausted
							gameOver();
					}
				} else {
					//user has entered the abort code
					gameAbort();
				}
				
			} else if (length>4){

				// cancel the entries
				guess[guesscount-1]="";
				numberofxcoordinates=0;
				promptForGuess(guesscount);
				playSound("toomanydigits");

			} else {
				updateGuessDisplay(guesscount);
				playDigits(newresult);
			}
		}

		this.icrContext.clearStrokes();
	}

    /**
     * When the ICR engine detects an acceptable series or strokes,
     * it prints the detected characters onto the Pulse display.
     */
	public void hwrResult(long time, String result) {
 	}

    /**
     * Called when an error occurs during handwriting recognition 
     */
	public void hwrError(long time, String error) {}

    /**
     * Called when the user crosses out text
     */
	public void hwrCrossingOut(long time, String result) {
	}
	
    /**
     * Specifies that the penlet should respond to events
     * related to open paper
     */
    public boolean canProcessOpenPaperEvents () {
        return true;
    }
	public boolean handleMenuEvent(MenuEvent menuEvent) {

		/* if current menu selection is not help, then reset
		 * the boolean that tracks if the help image is on the
		 * screen. this is done because we want to get back to 
		 * the menu when menu_left is detected when the help image
		 * is on the screen
		 */
		if (currentMenuSelection != menuHelpPos){
			menuHelpOnDisplay=false;
		}

		/* if menu has already been selected, then all events
		 * other than right, should come back to the menu again
		 */
		
		if (currentMenuSelection != menuHelpPos){
			menuHelpOnDisplay=false;
		}
		
		if (gamestate==gameinprogress){
			
			/* when the game is in progress, hitting the nav plus
			 * up and down will show the user the previous results
			 * if the user hits left, then the game is paused, the
			 * last menu item is displayed
			 */
			switch (menuEvent.eventId){
			case MenuEvent.MENU_DOWN:
				return displayResult(1);
			case MenuEvent.MENU_UP:
				return displayResult(-1);
			case MenuEvent.MENU_LEFT:
		        this.display.setCurrent(this.menuBrowseList);
		        currentMenuSelection=this.menuBrowseList.getFocusIndex();
		        playMenuPrompt(currentMenuSelection);
		        gamestate=gamepaused;
				return true;
			default:
				return false;
			}
		}
		else {
			switch (menuEvent.eventId) {
			case MenuEvent.MENU_DOWN:
				if (gamestate!=gameover){
					currentMenuSelection=this.menuBrowseList.focusToNext();
					this.display.setCurrent(this.menuBrowseList);
					playMenuPrompt(currentMenuSelection);
					break;
				} else { 
					return false;
				}
			case MenuEvent.MENU_UP:
				if (gamestate!=gameover){
					currentMenuSelection=this.menuBrowseList.focusToPrevious();
					this.display.setCurrent(this.menuBrowseList);
					playMenuPrompt(currentMenuSelection);
					break;
				} else { 
					return false;
				}
			case MenuEvent.MENU_RIGHT:
				if(gamestate!=gameover){
					currentMenuSelection=this.menuBrowseList.getFocusIndex();
					switch (currentMenuSelection){
					case 0:
						gamestate=newGame(true);
						break;
					case 1:
						soundToggle();
						this.display.setCurrent(this.menuBrowseList);					
						break;
					case 2:
						gameModeToggle();
						this.display.setCurrent(this.menuBrowseList);
						break;
					case 3:
						help();
						menuHelpOnDisplay=true;
						break;
					default:
					}
				} else {
					return false;
				}
				break;
			case MenuEvent.MENU_LEFT:
				if (gamestate==gameover || gamestate==makegameboard){
			        this.display.setCurrent(this.menuBrowseList);
			        currentMenuSelection=this.menuBrowseList.getFocusIndex();
			        playMenuPrompt(currentMenuSelection);
			        gamestate=0;
					return true;
				}
				
				if (currentMenuSelection==menuHelpPos){
					if (menuHelpOnDisplay){
						menuHelpOnDisplay=false;
				        this.display.setCurrent(this.menuBrowseList);
				        playMenuPrompt(currentMenuSelection);
				        return true;
					}
				}		
				return false;
			case MenuEvent.MENU_SELECT:
				return false;
			default: 
				currentMenuSelection=this.menuBrowseList.getFocusIndex();
				this.display.setCurrent(this.menuBrowseList);
			}
			return true;
		}
	}
	public void initializeGame(){
		
        //initialize the code, guesses and results
        
        for (int i=0;i<10;i++){
        	guess[i]="";
        	result[i]="";
        }
        codetocrack="";
        guesscount=guesstodisplay=1;
        guessRegionDefined=clearRegionDefined=soundRegionDefined=newGameRegionDefined=gameBoardReady=false;
	}
	
	public int newGame(boolean playgameboardprompt) {
		Random randgen;
		int codedigit=0;
		String[] filenames;
		
		filenames=new String[1];
		initializeGame();
		
		//create the code
		for (int j=0;j<9;j++){
			codedigitcount[j]=0;
		}
		randgen=new Random();
		for (int digcount=0;digcount<4;digcount++){
			boolean findcomplete=false;
			while (!findcomplete) {
				codedigit=randgen.nextInt(9);
				if (codedigitcount[codedigit]==0 || gamemode==gameModeAdvanced){
					
					/*check to make sure that we do not have 
					4 zeroes (the abort code) */
					
					if (!(codedigit==0 && codedigitcount[codedigit]==3)) {
						findcomplete=true;
						codedigitcount[codedigit]++;
					}
					
				}
			}
			codetocrack+=Integer.toString(codedigit);
		}
		
		displayLabel(imageGameBoard,"");
		if (playgameboardprompt){
			filenames[0]="rectangle";
			playSound(filenames);
		}

		return startgameboard;
		
	}	
	
	public void updateGuessDisplay(int count){
		String displayText="";
		
		displayText="#"+Integer.toString(count);
		if (guess[count-1].length()!=0){
			displayText+=":"+guess[count-1];
		}
		this.label.draw(displayText,true);

		if (result[count-1].length()!=0){
			showResult(count);
		}
		
		displayOptions();
			
		this.display.setCurrent(this.label);
				
	}
	
	public void showResult(int count){
		Image img=null;
		int imageX,imageY=0;
		
		//6 for system tray, and then account for all 4 results
		imageX=89-imageBull.getWidth()*4-imageInter.getWidth();

		if (gamemode==gameModeBasic){
			for (int i=0;i<result[count-1].length();i++){
				switch (result[count-1].charAt(i)){
				case 'b': img=imageBull; break;
				case 'w': img=imageCow;break;
				case '-': img=imageWrong;break;
				}
				displayImageAtXY(img,imageX,imageY);
				imageX+=img.getWidth();
			}
		} else {
			//number of bulls
			int bulls=Character.digit(result[count-1].charAt(0),10);
			int cows=Character.digit(result[count-1].charAt(2),10);
			int imageXB=imageX,imageXW=imageX;
			int imageYB=0,imageYW=0;
			
			for (int i=0;i<bulls;i++){
				displayImageAtXY(imageBull,imageXB,imageYB);
				imageXB+=imageBull.getWidth();
			}
			
			if (bulls!=0){
				imageYW=imageBull.getHeight();
			}

			for (int i=0;i<cows;i++){
				displayImageAtXY(imageCow,imageXW,imageYW);
				imageXW+=imageCow.getWidth();
			}
			
			if (bulls==0 && cows==0){
				displayImageAtXY(imageWrong,imageX,imageY);
			}
		}
		
	}
	public void displayOptions(){
		Image img;

		if (gamemode==gameModeBasic){
			img=imageBasic;
		} else if (gamemode==gameModeInter){
			img=imageInter;
		} else {
			img=imageAdvanced;
		};
		
		//account for system tray!
		displayImageAtXY(img,89-img.getWidth(),0);
		
	}
	public void promptForGuess(int count){

		
		updateGuessDisplay(count);	
		playGuessPrompt(count);
		if (guess[count-1].length()!=0){
				playGuess(count);
		} else {
			/* there are no digits in the guess. reset the
			 * value that holds the xcoordinates of all the digits
			 * written by the user. this is used to sort the data to account for
			 * the user having written digits out of order 
			 */
			numberofxcoordinates=0;
		}

		if (result[count-1].length()!=0){
			String[] audiofiles;	
			audiofiles=getAudioResourcesForResult(count);
			playSound(audiofiles);
		}

	}
	
	public void codeBroken(){
		this.label.draw("Congrats:#"+Integer.toString(guesscount)+" "+codetocrack,true);
		this.display.setCurrent(this.label);
		playSound("clap");
		gamestate=gameover;
	}

	public void gameOver(){
		this.label.draw("Sorry! Code is "+codetocrack,true);
		this.display.setCurrent(this.label);
		playSound("SL_Scroll");
		gamestate=gameover;
	}
	
	public void gameAbort(){
		this.label.draw("Code is "+codetocrack,true);
		this.display.setCurrent(this.label);
		playSound("SL_Cancel");
		gamestate=gameover;
	}

	public void displayLabel(Image img, String txt){
		this.label.draw(img,txt,true);
		this.display.setCurrent(this.label);
	}

	public void displayLabel(String txt,Image img){
		this.label.draw(txt,img,true);
		this.display.setCurrent(this.label);
	}

	public void displayImageAtXY(Image img,int x,int y){
		this.label.draw("",img,x,y, img.getWidth(),img.getHeight(),true);
		this.display.setCurrent(this.label);
	}
	

	public boolean displayResult(int upordown){
		guesstodisplay+=upordown;
		if (guesstodisplay>guesscount || guesstodisplay <1) {
			return false;
		} else {
			promptForGuess(guesstodisplay);
		}
		return true;
	}

	public void playSound(String soundname){
		if (soundon){
			soundname="/audio/"+soundname+".wav";
			this.player.play(soundname,false);
		}
	}

	public void playSound(String[] soundnamearray){
		if (soundon){
			for (int j=0;j<soundnamearray.length;j++){
				soundnamearray[j]="/audio/"+soundnamearray[j]+".wav";				
			}
			this.player.play(soundnamearray,false);
		}
	}
	public void playGuessPrompt(int count){
		String filename;
		switch (count){
		case 1: 
			if (result[count-1].length()==0){
				filename="firstwithhelp";
			} else {
				filename="first";
			}
			break;
		case 2: filename="second";break;
		case 3: filename="third";break;
		case 4: filename="fourth";break;
		case 5: filename="fifth";break;
		case 6: filename="sixth";break;
		case 7: filename="seventh";break;
		case 8: filename="eighth";break;
		case 9: filename="ninth";break;
		case 10: filename="tenth";break;
		default : filename="SL_Error";
		}
		playSound(filename);
		
	}
	public void playGuess(int count){
		
		if (soundon){
			//play the guess and the result
			playDigits(guess[count-1]);
		}
	}
	
	public void playDigits (String str){
		if (soundon){
			String[] filenames;
			filenames=new String[str.length()];
			for (int j=0;j<str.length();j++){

				switch (str.charAt(j)){
				case '1': filenames[j]="one";break;
				case '2': filenames[j]="two";break;
				case '3': filenames[j]="three";break;
				case '4': filenames[j]="four";break;
				case '5': filenames[j]="five";break;
				case '6': filenames[j]="six";break;
				case '7': filenames[j]="seven";break;
				case '8': filenames[j]="eight";break;
				case '9': filenames[j]="nine";break;
				case '0': filenames[j]="zero";break;
				default : filenames[j]="wrong";
				}
			}
			playSound(filenames);
		}
	}
	
	public String[] getAudioResourcesForResult(int count){
		
			//get the resources for the result
			String filename;
			String[] filenames;
			
			filenames=new String[result[count-1].length()];
			for (int j=0;j<result[count-1].length();j++){
				switch (result[count-1].charAt(j)){
				case '1': filename="one";break;
				case '2': filename="two";break;
				case '3': filename="three";break;
				case '4': filename="four";break;
				case '5': filename="five";break;
				case '6': filename="six";break;
				case '7': filename="seven";break;
				case '8': filename="eight";break;
				case '9': filename="nine";break;
				case '0': filename="zero";break;
				case 'b': filename="black";break;
				case 'w': filename="white";break;
				case '-': filename="nomatch";break;
				default : filename="SL_Error";
				}
				filenames[j]=filename;
			}		
			return filenames;
	}

	public void playMenuPrompt (int menuselection){
		if (soundon){
			playSound(menusounds[menuselection]);	
		}
	}
	
	public int addRegionToGameBoard(){
		
		if (!guessRegionDefined){
			guessRegionDefined=true;
			return 1;
		} else {
			if (!clearRegionDefined){
				clearRegionDefined=true;
				return 2;
			} else {
				if (!soundRegionDefined){
					soundRegionDefined=true;
					return 3;
				} else {
					newGameRegionDefined=true;
					return 4;
				}
			}
		}
	}

	private void playDrawGameBoardPrompt() {
		if (!clearRegionDefined){
			displayLabel(imageClearButton,"");
			playSound("clearicon");
			return;
		}
		if (!soundRegionDefined){
			displayLabel(imageSoundButton,"");
			playSound("soundicon");
			return;
		}
		if (!newGameRegionDefined){
			displayLabel(imageNewGameButton,"");
			playSound("soundicon");
			playSound("newgameicon");
			return;
		}
		if (!gameBoardReady){  
			gameBoardReady=true;
			gamestate=gameinprogress;
			promptForGuess(guesscount);
			return;
		}
		return;
	}
	
	public void useExistingGameBoard(){

        guessRegionDefined=clearRegionDefined=soundRegionDefined=newGameRegionDefined=gameBoardReady=true;
		gamestate=gameinprogress;
		promptForGuess(guesscount);		
	}
	public void penUp(long time, Region areaID, PageInstance page) {
	}
	
	
	public void penDown(long time, Region areaID, PageInstance page) {
		int id;
		
		id=areaID.getAreaId();
		if (id!=0 && gameBoardReady){
			if (clearRegion==id){
				/* User has tapped the "clear icon". 
				 * check to see if the game is over?
				 * if there are entries in the current guess. 
				 * clear them. else clear the previous entry
				 */
				if (gamestate==gameinprogress){
					if (guess[guesscount-1].length()>0){
						guess[guesscount-1]="";
						result[guesscount-1]="";
					} else if (guesscount>1){
						guesscount--;
						guess[guesscount-1]="";
						result[guesscount-1]="";
					}
					promptForGuess(guesscount);
				}
				return;
			} 
			
			if (soundRegion==id){
				soundToggle();
				return;
			}
			
			if (newGameRegion==id){
				if (gamestate==0 || gamestate==actapp || gamestate==gameover || gamestate==gameinprogress || gamestate==gamepaused){
					gamestate=newGame(true);
				}
				return;
			}
			
			if (guessRegion==id && gamestate==gamepaused){
				gamestate=gameinprogress;
				promptForGuess(guesscount);
				return;
			}

			if (guessRegion==id && gamestate==gameinprogress){
				if (this.player.isAudioPlayerPlaying()){
					this.player.stop();
				}
				return;
			}
		}
	}

	public void soundToggle(){
		soundon=!soundon;
		if (soundon){
			playSound("soundon");
		} else {
			soundon=true;
			playSound("soundoff");
			soundon=false;
		}		
		updateSoundMenu();
	}
	public void gameModeToggle(){

		if (gamemode==gameModeBasic){
	        gamemode=gameModeInter;	
			playSound("intermediategamemode");
		} else if (gamemode==gameModeInter){
	        gamemode=gameModeAdvanced;		
			playSound("advancedgamemode");
		} else if (gamemode==gameModeAdvanced){
			gamemode=gameModeBasic;
			playSound("basicgamemode");
		}
		updateGameModeMenu();
	}
	
	public void help(){
		playSound("helpdetails");
		displayLabel("",imageHelp);
	}
	
	public void singleTap(long time, int x, int y) {
		singletaptime=time;
				
		// the first single tap comes from the menu system, so we ignore it
		if (gamestate==startgameboard){
			gamestate=makegameboard;
			makingRegionRect=true;
		}
	}
	public void doubleTap(long time, int x, int y) {
		if (gamestate==makegameboard){
			if (!guessRegionDefined && makingRegionRect){
				/* double tap at initial prompt to draw the gameboard.
				 * this implies that user wants to use existing one
				 */
				
				useExistingGameBoard();
			} else {

				/* Prompt for the next piece of the 
				 * gameboard.
				 */
				
				RegionCollection rc=this.context.getCurrentRegionCollection();
				int id=addRegionToGameBoard();
				Region rg=new Region(id,false,true);
				rg=rc.addRegion(RegionRect, rg);
				makingRegionRect=true;
				playDrawGameBoardPrompt();
				
			}
		}
	}
	
	

}

  