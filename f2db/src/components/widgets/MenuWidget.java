package components.widgets;

import java.util.ArrayList;
import java.util.List;

import modules.misc.Constants;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * This class is a container for a ToolItem and a MenuItem. But it is not a Widget.
 * 
 * @author Sebastian Seifert
 *
 */
public class MenuWidget {
	
	private ToolItem toolItem;
	private MenuItem menuItem;
	
	private List<ListenerEntry> listenerList = new ArrayList<ListenerEntry>();
	
//	private ToolBar toolBar;
//	private Menu menu;
	
	private static List<MenuWidget> list = new ArrayList<MenuWidget>();
	
	//submenu items
	private Menu subMenuToolBar;
	private List<MenuItem> subMenuItems = null;
	private Listener subMenuListener = null;
	
	
	public MenuWidget(ToolBar toolBar, Menu menu, int styleTI, int styleMI){
		if (toolBar == null || menu == null)
			throw new NullPointerException();
		
//		this.toolBar = toolBar;
//		this.menu = menu;
		
		toolItem = new ToolItem(toolBar, styleTI);
		menuItem = new MenuItem(menu, styleMI);
		list.add(this);
	}

	public void addListener(int eventType, Listener listener) {
		toolItem.addListener(eventType, listener);
		menuItem.addListener(eventType, listener);
		//register listener
		listenerList.add(new ListenerEntry(eventType, listener));
	}

	public void removeListener(int eventType, Listener listener) {
		toolItem.removeListener(eventType, listener);
		menuItem.removeListener(eventType, listener);
		//remove Listener
		for (int i = 0; i < listenerList.size(); i++)
		{
			if (listenerList.get(i).getEventType() == eventType && listenerList.get(i).getListener() == listener)
			{
				listenerList.remove(i);
				i--;
			}
		}
	}

	public Image getImage() {
		// or from toolItem
		return menuItem.getImage();
	}

	public Menu getMenu() {
		return menuItem.getMenu();
	}
	
	public ToolBar getTollItemParent() {
		return toolItem.getParent();
	}

	public Menu getMenuItemParent() {
		return menuItem.getParent();
	}

	public int getToolItemStyle() {
		return toolItem.getStyle();
	}

	public int getMenuItemStyle() {
		return menuItem.getStyle();
	}

	public void setEnabled(boolean enabled) {
		toolItem.setEnabled(enabled);
		menuItem.setEnabled(enabled);
	}

	public void setImage(Image image) {
		toolItem.setImage(image);
		menuItem.setImage(image);
	}

	public void setText(String string) {
		toolItem.setText(string);
		menuItem.setText(string);
	}

	public Image getDisabledImage() {
		return toolItem.getDisabledImage();
	}

	public Image getHotImage() {
		return toolItem.getHotImage();
	}

	public void setDisabledImage(Image image) {
		toolItem.setDisabledImage(image);
	}

	public void setHotImage(Image image) {
		toolItem.setHotImage(image);
	}

	public void setToolTipText(String string) {
		toolItem.setToolTipText(string);
	}

	public boolean getEnabled() {
		// or from toolItem
		return menuItem.getEnabled();
	}
	
	public void setMenu(Menu menu){
		menuItem.setMenu(menu);
	}

	public int getWidth() {
		return toolItem.getWidth();
	}

	public void addDisposeListener(DisposeListener listener) {
		toolItem.addDisposeListener(listener);
		menuItem.addDisposeListener(listener);
		//register listener
		listenerList.add(new ListenerEntry(SWT.Dispose, (Listener) listener));
	}

	public void dispose() {
		toolItem.dispose();
		menuItem.dispose();

		list.remove(this);
	}

	public boolean equals(Object obj) {
		return toolItem.equals(obj) && menuItem.equals(obj);
	}

	public Object getToolItemData() {
		return toolItem.getData();
	}

	public Object getMenuItemData() {
		return menuItem.getData();
	}

	public Object getToolItemData(String key) {
		return toolItem.getData(key);
	}

	public Object getMenuItemData(String key) {
		return menuItem.getData(key);
	}

	public boolean isDisposed() {
		return toolItem.isDisposed() && menuItem.isDisposed();
	}

//	public boolean isListening(int eventType) {
//		return item.isListening(eventType);
//	}

	public void notifyListeners(int eventType, Event event) {
		toolItem.notifyListeners(eventType, event);
		menuItem.notifyListeners(eventType, event);
	}

	public void removeDisposeListener(DisposeListener listener) {
		toolItem.removeDisposeListener(listener);
		menuItem.removeDisposeListener(listener);
		//remove dispose listener
		for (int i = 0; i < listenerList.size(); i++)
		{
			if (listenerList.get(i).getEventType() == SWT.Dispose && listenerList.get(i).getListener() == (Listener) listener)
			{
				listenerList.remove(i);
				i--;
			}
		}
	}

	public ToolItem getToolItem() {
		return toolItem;
	}

	public MenuItem getMenuItem() {
		return menuItem;
	}

	public void cleanSubMenuItems() {
		subMenuItems = null;
	}
	
	public void addSubMenuItem(MenuItem intermentItem) {
		if (subMenuItems == null)
			subMenuItems = new ArrayList<MenuItem>();
		subMenuItems.add(intermentItem);
	}

	public void setSubMenuListener(Listener listener) {
		this.subMenuListener = listener;
	}

	public static MenuWidget getMenuWidget(MenuItem mi) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).menuItem == mi)
				return list.get(i);
		}
		return null;
	}
	
	public void setNewInMenu(Menu menu)
	{
		int miStyle;
		switch (toolItem.getStyle())
		{
		case SWT.PUSH:
			miStyle = SWT.NONE;
			break;
		case SWT.DROP_DOWN:
			miStyle = SWT.CASCADE;
			break;
		default:
			miStyle = menuItem.getStyle();
		}	
		MenuItem newMenuItem = new MenuItem(menu, miStyle);
		newMenuItem.setImage(menuItem.getImage());
		newMenuItem.setEnabled(menuItem.getEnabled());
		newMenuItem.setText(menuItem.getText());
		if (menuItem.isDisposed())
			newMenuItem.dispose();
		for (int i = 0; i < listenerList.size(); i++)
		{
			if (this.toolItem.getStyle() == SWT.DROP_DOWN && /*listenerList.get(i).getEventType() == SWT.Selection && */ subMenuItems != null)
			{
				Menu subMenu = new Menu(toolItem.getParent().getShell(), SWT.DROP_DOWN);
				newMenuItem.setMenu(subMenu);
				int subMenuStyle = SWT.CASCADE;
				int active = -1;
				if (newMenuItem.getText().equals(Constants.main_toolButtonNames[4]))
				{
					subMenuStyle |= SWT.RADIO;
					for (int j = 0; j < subMenuToolBar.getItemCount(); j++)
					{
						if (subMenuToolBar.getItem(j).getSelection())
							active = j;
					}
					MenuItem subMenuItem = new MenuItem(subMenu, subMenuStyle);
					if (menuItem.getImage() == ResourceRegistry.getInstance().getImage("executionon"))
						subMenuItem.setText(" off");
					else
						subMenuItem.setText(" on");
					subMenuItem.addListener(listenerList.get(i).getEventType(), listenerList.get(i).getListener());
				}
				for (int j = 0; j < subMenuItems.size(); j++)
				{
					MenuItem subMenuItem = new MenuItem(subMenu, subMenuStyle);
					subMenuItem.setText(subMenuItems.get(j).getText());
					subMenuItem.addListener(SWT.Selection, subMenuListener);
				}
				if (newMenuItem.getText().equals(Constants.main_toolButtonNames[4]) && active >= 0)
					subMenu.getItem(active + 1).setSelection(true);
			}
			else
			{
				newMenuItem.addListener(listenerList.get(i).getEventType(), listenerList.get(i).getListener());
			}
		}
	}
	
	public void setSubMenu(Menu subMenuToolBar)
	{
		this.subMenuToolBar = subMenuToolBar;
	}
}

class ListenerEntry {
	private int eventType;
	private Listener listener;
	
	public ListenerEntry(int eventType, Listener listener) {
		this.eventType = eventType;
		this.listener = listener;
	}
	
	public int getEventType()
	{
		return eventType;
	}
	
	public Listener getListener()
	{
		return listener;
	}
}