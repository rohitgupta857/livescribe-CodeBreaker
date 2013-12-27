package net.quasarsolutions.penlet.utils;

import java.io.InputStream;

import com.livescribe.display.BrowseList;
import com.livescribe.display.Image;
import com.livescribe.penlet.Logger;
import com.livescribe.i18n.ResourceBundle;

public class MenuBrowseListItem implements BrowseList.Item{
	
	boolean isMenuSelectable;
	InputStream MenuSound;
	Image MenuIcon;
	String MenuTitle;
	Logger Log;
	public MenuBrowseListItem(Logger log,boolean selectable, InputStream stream, Image icon, String title){

		isMenuSelectable=selectable;
		MenuIcon=icon;
		MenuSound=stream;
		MenuTitle=title;
		Log=log;
	}

	public void updateTitle(String title){
		MenuTitle=title;
	}
	
	public boolean isSelectable(){
		return isMenuSelectable;
	}
	
	public Image getIcon() {
		return MenuIcon;
	}
	
	public Object getText(){
		return MenuTitle;
	}
	
	public InputStream getAudioStream(){
		return MenuSound;
	}

	// abstract interface implementation for BrowseList.Item
	public String getAudioMimeType(){
		
		return ResourceBundle.MIME_AUDIO_WAV ;
	}

}

