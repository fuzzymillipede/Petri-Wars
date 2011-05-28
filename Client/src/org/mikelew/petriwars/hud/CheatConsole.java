package org.mikelew.petriwars.hud;

import java.util.ArrayList;
import java.util.LinkedList;

import org.jagatoo.input.devices.components.DeviceComponent;
import org.jagatoo.input.devices.components.Key;
import org.jagatoo.input.devices.components.Keys;
import org.jagatoo.logging.LogChannel;
import org.jagatoo.logging.LogInterface;
import org.jagatoo.logging.LogLevel;
import org.openmali.types.twodee.Dim2f;
import org.openmali.vecmath2.Colorf;
import org.xith3d.scenegraph.Texture2D;
import org.xith3d.scenegraph.Texture2DCanvas;
import org.xith3d.ui.hud.HUD;
import org.xith3d.ui.hud.base.BackgroundSettableWidget;
import org.xith3d.ui.hud.base.Border;
import org.xith3d.ui.hud.base.Widget;
import org.xith3d.ui.hud.borders.BorderFactory;
import org.xith3d.ui.hud.listeners.TextFieldListener;
import org.xith3d.ui.hud.listeners.WidgetKeyboardAdapter;
import org.xith3d.ui.hud.utils.HUDFont;
import org.xith3d.ui.hud.utils.PopUpable;
import org.xith3d.ui.hud.utils.ScrollMode;
import org.xith3d.ui.hud.utils.TileMode;
import org.xith3d.ui.hud.utils.WidgetMover;
import org.xith3d.ui.hud.widgets.List;
import org.xith3d.ui.hud.widgets.TextField;

/**
 * A Console is what you will know from many kinds of games mostly from FPS
 * games. Logging is dumped to it and you can type commands.
 * 
 * @author Marvin Froehlich (aka Qudus) (edited by Tim Pittman)
 */
public class CheatConsole extends BackgroundSettableWidget implements PopUpable, LogInterface {
	private static final float POPUP_SPEED = 1.5f;
	private static final int COMMANDHISTORY_MAXSIZE = 20;
	
	private Boolean initiallyVisible;
	private int logLevel;
	private int channelFilter;
	private final Colorf errorColor;
	private final Colorf warningColor;
	private final Colorf normalColor;
	private final Colorf commandColor;
	private final List list;
	private final float listInputBoxGap;
	private final TextField inputBox;
	private final List commandPreview;
	private boolean isPoppedUp = true;
	private boolean trimMessages = true;

	private int historylocation = -1;
	private final LinkedList<String> enteredCommands = new LinkedList<String>();
	private final ArrayList<ConsoleListener> listeners = new ArrayList<ConsoleListener>();
	
	public CheatConsole(float width, float height, int channelFilter, int logLevel, Description desc, boolean initiallyVisible) {
		super(true, true, desc.getBackgroundColor(), desc.getBackgroundTexture(), TileMode.TILE_BOTH);

		this.logLevel = logLevel;
		this.channelFilter = channelFilter;

		this.normalColor = new Colorf(desc.getListFontColorNormal());
		this.warningColor = new Colorf(desc.getListFontColorWarning());
		this.errorColor = new Colorf(desc.getListFontColorError());
		this.commandColor = new Colorf(desc.getListFontColorCommand());

		if (desc.getBorderDescription() != null)
			this.setBorder(BorderFactory.createBorder(desc.getBorderDescription()));

		float innerWidth = getWidth();
		float innerHeight = getHeight();

		this.listInputBoxGap = desc.getListInputBoxGap();

		if (desc.getInputBoxDescription() == null)
			this.inputBox = new InputBox(innerWidth, 20f);
		else
			this.inputBox = new InputBox(innerWidth, 20f, desc.getInputBoxDescription());
		inputBox.setAutoSizeEnabled(false);
		inputBox.setIgnoredChars('^');

		if (desc.getListDescription() == null)
			this.list = new List(innerWidth, innerHeight - inputBox.getHeight()
					- listInputBoxGap, null);
		else
			this.list = List.newTextList(innerWidth, innerHeight - inputBox.getHeight()
					- listInputBoxGap, desc.getListDescription());
		list.setFixedToBottom(true);

		this.commandPreview = List.newTextList(true, 120f, 100f,
				desc.getPreviewListDescription());
		commandPreview.setScrollMode(ScrollMode.NEVER);
		commandPreview.setFontColor(inputBox.getFontColor());

		this.initiallyVisible = new Boolean(initiallyVisible);

		setSize(width, height);

		setZIndex(HUD.CURSOR_Z_INDEX - 10);
	}

	public CheatConsole(float width, float height, int channelFilter, Description desc, boolean initiallyVisible) {
		this(width, height, channelFilter, LogLevel.REGULAR, desc, initiallyVisible);
	}

	public CheatConsole(float width, float height, Description desc, boolean initiallyVisible) {
		this(width, height, LogChannel.MASK_ALL, LogLevel.REGULAR, desc, initiallyVisible);
	}

	public CheatConsole(float width, float height, int channelFilter, int logLevel,
			boolean initiallyVisible) {
		this(width, height, channelFilter, logLevel, new Description(), initiallyVisible);
	}

	public CheatConsole(float width, float height, int channelFilter, boolean initiallyVisible) {
		this(width, height, channelFilter, LogLevel.REGULAR, initiallyVisible);
	}

	public CheatConsole(float width, float height, boolean initiallyVisible) {
		this(width, height, LogChannel.MASK_ALL, LogLevel.REGULAR, initiallyVisible);
	}

	private final WidgetMover mover = new WidgetMover(this) {
		@Override protected void onMovementStopped() {
			if (!isPoppedUp()) {
				// Set invisible to save performance!
				CheatConsole.this.setVisible(false);

				if (getHUD() != null) {
					getHUD().disposeFocus();
				}
			}
		}
	};

	/**
	 * Adds a {@link ConsoleListener} to the list of notified listeners.
	 * 
	 * @param l
	 */
	public final void addConsoleListener(ConsoleListener l) {
		listeners.add(l);
	}

	/**
	 * Removes a {@link ConsoleListener} from the list of notified listeners.
	 * 
	 * @param l
	 */
	public final void removeConsoleListener(ConsoleListener l) {
		listeners.remove(l);
	}

	private final boolean compare(float v1, float v2) {
		return (Math.abs(v2 - v1) < (this.getWidth() + this.getHeight()) / 1000f);
	}

	private final void popUp(boolean p, boolean jump) {
		//if (!p) { setCommandPreviewListVisible(false); }

		final HUD hud = getHUD();

		boolean doIt = false;

		if (!mover.isMoving()) {
			if (isPoppedUp() && !p) {
				// fade out...
				if ((compare(this.getTop(), 0f)) && (compare(this.getLeft(), 0f))
						&& (compare(this.getWidth(), hud.getResX()))) {
					// fade out to top in half a second
					mover.setDestinationLocation(0f, -this.getHeight());
					mover.setSpeed(this.getHeight() * POPUP_SPEED);
					doIt = true;
				} else if ((compare(this.getTop(), 0f)) && (compare(this.getLeft(), 0f))
						&& (compare(this.getHeight(), hud.getResY()))) {
					// fade out to left in half a second
					mover.setDestinationLocation(-this.getWidth(), 0f);
					mover.setSpeed(this.getWidth() * POPUP_SPEED);
					doIt = true;
				} else if ((compare(this.getTop(), hud.getResY() - this.getHeight()))
						&& (compare(this.getLeft(), 0f))
						&& (compare(this.getWidth(), hud.getResX()))) {
					// fade out to bottom in half a second
					mover.setDestinationLocation(0f, hud.getResY());
					mover.setSpeed(this.getHeight() * POPUP_SPEED);
					doIt = true;
				} else if ((compare(this.getTop(), 0f))
						&& (compare(this.getLeft(), hud.getResX() - this.getWidth()))
						&& (compare(this.getHeight(), hud.getResY()))) {
					// fade out to right in half a second
					mover.setDestinationLocation(hud.getResX(), 0f);
					mover.setSpeed(this.getWidth() * POPUP_SPEED);
					doIt = true;
				}
			} else if (!isPoppedUp() && p) {
				// fade in...

				if (this.getTop() < -(this.getHeight() / 2f)) {
					// fade in from top in half a second
					mover.setDestinationLocation(0f, 0f);
					mover.setSpeed(this.getHeight() * POPUP_SPEED);
					doIt = true;
				} else if (this.getLeft() < -(this.getWidth() / 2f)) {
					// fade in from left in half a second
					mover.setDestinationLocation(0f, 0f);
					mover.setSpeed(this.getWidth() * POPUP_SPEED);
					doIt = true;
				} else if (this.getTop() > hud.getResY() - (this.getHeight() / 2f)) {
					// fade in from bottom in half a second
					mover.setDestinationLocation(0f, hud.getResY() - this.getHeight());
					mover.setSpeed(this.getHeight() * POPUP_SPEED);
					doIt = true;
				} else if (this.getLeft() > hud.getResX() - (this.getWidth() / 2f)) {
					// fade in from right in half a second
					mover.setDestinationLocation(hud.getResX() - this.getWidth(), 0f);
					mover.setSpeed(this.getWidth() * POPUP_SPEED);
					doIt = true;
				}
			}
		}

		if (doIt) {
			if (!jump) {
				if (!isPoppedUp() && p) {
					// force visibility
					this.setVisible(true);

					if (isInputBoxVisible())
						inputBox.requestFocus();
					else
						list.requestFocus();
				}

				mover.startMoving();
			} else {
				this.setLocation(mover.getDestinationLocation());

				if (!p)
					hud.disposeFocus();
			}
		}

		isPoppedUp = p;
	}

	/**
	 * {@inheritDoc}
	 */
	public void popUp(boolean p) { popUp(p, false); }

	/**
	 * {@inheritDoc}
	 */
	public boolean isPoppedUp() { return (isPoppedUp); }

	/**
	 * Sets the color to be used for the given LogLevel.<br>
	 * Currently ERROR, EXCEPTION and REGULAR use distinct colors and DEBUG,
	 * EXHAUSTIVE and PROFILE will use the REGULAR color.
	 * 
	 * @param logLevel
	 * @param color
	 */
	public void setListFontColor(int logLevel, Colorf color) {
		if (color == null)
			throw new NullPointerException("color");

		switch (logLevel) {
		case LogLevel.ERROR:
			errorColor.set(color);
			return;
		case LogLevel.EXCEPTION:
			warningColor.set(color);
			return;
		case LogLevel.DEBUG:
		case LogLevel.EXHAUSTIVE:
		case LogLevel.PROFILE:
		case LogLevel.REGULAR:
			normalColor.set(color);
			return;
		}

		throw new IllegalArgumentException("This LogLevel is invalid.");
	}

	/**
	 * @return the color to be used for the given LogLevel.<br>
	 *         Currently ERROR, EXCEPTION and REGULAR use distinct colors and
	 *         DEBUG, EXHAUSTIVE and PROFILE will use the REGULAR color.
	 * 
	 * @param logLevel
	 */
	public Colorf getListFontColor(int logLevel) {
		switch (logLevel) {
		case LogLevel.ERROR:
			return (errorColor);
		case LogLevel.EXCEPTION:
			return (warningColor);
		case LogLevel.DEBUG:
		case LogLevel.EXHAUSTIVE:
		case LogLevel.PROFILE:
		case LogLevel.REGULAR:
			return (normalColor);
		}

		throw new IllegalArgumentException("This LogLevel is invalid.");
	}

	public final void setLogLevel(int logLevel) { this.logLevel = logLevel; }
	public final int getLogLevel() { return logLevel; }

	public final void setChannelFilter(int filter) { this.channelFilter = filter; }
	public final int getChannelFilter() { return channelFilter; }

	public final boolean acceptsChannel(LogChannel channel) {
		return ((channelFilter & channel.getID()) > 0);
	}

	public boolean println(String message, Colorf color) {
		if (trimMessages) message = message.trim();

		if (message.length() > 0) {
			list.addItem(message, color);
			list.scrollToBottom();
			return true;
		}
		return false;
	}

	public final boolean println(int logLevel, String message) {
		return (println(message, getListFontColor(logLevel)));
	}

	public final boolean println(String message) {
		return (println(LogLevel.REGULAR, message));
	}

	/**
	 * {@inheritDoc}
	 */
	public final void print(LogChannel channel, int logLevel, String message) {
		if ((acceptsChannel(channel)) && (logLevel <= this.logLevel)) {
			println(logLevel, message);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void println(LogChannel channel, int logLevel, String message) {
		if ((acceptsChannel(channel)) && (logLevel <= this.logLevel)) {
			println(logLevel, message);
		}
	}


	/**
	 * This event is fired when a command has been typed to the input-box.<br>
	 * 
	 * @param commandLine
	 *        the entered command
	 */
	protected void onCommandEntered(String commandLine) {
		commandLine = commandLine.trim();

		// notify listeners...
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).onCommandEntered(this, commandLine);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void flush() {}

	/**
	 * {@inheritDoc}
	 */
	public void close() {}

	/**
	 * Sets the array of chars ignored by this CheatConsole. This is especially
	 * useful when a printable char's key is used to popup the console.
	 * 
	 * @param ignoredChars
	 */
	public void setIgnoredChars(char... ignoredChars) {
		inputBox.setIgnoredChars(ignoredChars);
	}

	/**
	 * @return the array of chars ignored by this CheatConsole. This is
	 *         especially useful when a printable char's key is used to popup
	 *         the console.
	 */
	public char[] getIgnoredChars() {
		return (inputBox.getIgnoredChars());
	}

	/**
	 * Sets if the messages should be trimmed before display.<br>
	 * Default is 'true'.
	 * 
	 * @param trimMessages
	 *        'true' if messages should be trimmed.
	 */
	public void setTrimMessages(boolean trimMessages) {
		this.trimMessages = trimMessages;
	}

	/**
	 * @return 'true' if messages are trimmed before displaying them
	 */
	public boolean isTrimMessages() {
		return (trimMessages);
	}

	/**
	 * Defines, if the {@link CheatConsole}'s input-box is visible or hidden.
	 * 
	 * @param visible
	 */
	public void setInputBoxVisible(boolean visible) {
		this.inputBox.setVisible(visible);

		if (!visible && inputBox.hasFocus(true))
			list.requestFocus();

		update();
	}

	/**
	 * @return if the {@link CheatConsole}'s input-box is visible or hidden.
	 */
	public boolean isInputBoxVisible() {
		return (inputBox.isVisible());
	}

	@Override protected void onAttachedToHUD(HUD hud) {
		super.onAttachedToHUD(hud);

		if ((initiallyVisible != null) && !initiallyVisible.booleanValue()) {
			initiallyVisible = null;
			popUp(false, true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override protected void onSizeChanged(float oldWidth, float oldHeight, float newWidth,
			float newHeight) {
		final float innerWidth;
		final float innerHeight;
		if (getBorder() == null) {
			innerWidth = this.getWidth();
			innerHeight = this.getHeight();
		} else {
			innerWidth = this.getWidth() - getBorder().getLeftWidth()
					- getBorder().getRightWidth();
			innerHeight = this.getHeight() - getBorder().getTopHeight()
					- getBorder().getBottomHeight();
		}

		float inputBoxHeight = 0f;
		if (inputBox.isVisible()) {
			inputBox.setSize(innerWidth, inputBox.getHeight());
			inputBoxHeight = inputBox.getHeight();
			if (getBorder() != null)
				getWidgetAssembler()
						.reposition(
								inputBox,
								getBorder().getLeftWidth(),
								this.getHeight() - getBorder().getBottomHeight()
										- inputBox.getHeight());
			else
				getWidgetAssembler().reposition(inputBox, 0f,
						this.getHeight() - inputBox.getHeight());

/*			if (isCommandPreviewVisible()) {
				commandPreview.setLocation(commandPreview.getLeft(), commandPreview.getTop()
						+ (newHeight - oldHeight));
			} //*/
			float inputBoxLeft = getWidgetAssembler().getPositionX(inputBox);
			float inputBoxTop = getWidgetAssembler().getPositionY(inputBox);
			getWidgetAssembler().reposition(commandPreview, inputBoxLeft + 10f,
					inputBoxTop + inputBox.getHeight());
		}

		list.setSize(innerWidth, innerHeight - inputBoxHeight
				- (inputBox.isVisible() ? listInputBoxGap : 0f));

		if (getBorder() != null)
			getWidgetAssembler().reposition(list, getBorder().getLeftWidth(),
					getBorder().getTopHeight());
		else
			getWidgetAssembler().reposition(list, 0f, 0f);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override protected void drawWidget(Texture2DCanvas texCanvas, int offsetX, int offsetY,
			int width, int height, boolean drawsSelf) {}

	/**
	 * {@inheritDoc}
	 */
	@Override protected void init() {
		Dim2f buffer = Dim2f.fromPool();

		getSizePixels2HUD_(getContentLeftPX(), getContentTopPX(), buffer);
		float contentLeft = buffer.getWidth();
		float contentTop = buffer.getHeight();

		float contentBottom = 0f;
		if (getBorder() != null) {
			getSizePixels2HUD_(0, getBorder().getBottomHeight(), buffer);
			contentBottom = buffer.getHeight();
		}

		float inputBoxLeft = contentLeft;
		float inputBoxTop = this.getHeight() - contentBottom - inputBox.getHeight();

		getWidgetAssembler().addWidget(inputBox, inputBoxLeft, inputBoxTop);

		inputBox.getMinimalSize(buffer);
		float inputBoxHeight = inputBox.getHeight();

		Dim2f.toPool(buffer);

		float contentWidth = getContentWidth();

		inputBox.setSize(contentWidth, inputBoxHeight);
		list.setSize(contentWidth, getContentHeight() - inputBox.getHeight() - listInputBoxGap);

		getWidgetAssembler().addWidget(list, contentLeft, contentTop);

		inputBoxTop = this.getHeight() - contentBottom - inputBox.getHeight();

		getWidgetAssembler().reposition(inputBox, inputBoxLeft, inputBoxTop);

		getWidgetAssembler().setKeyEventsDispatched(true);
		getWidgetAssembler().setPickDispatched(true);

		inputBox.requestFocus();

		inputBox.addTextFieldListener(new TextFieldListener() {
			public void onCharTyped(TextField textField, char ch) {}
			public void onCharDeleted(TextField textField) {}
			public void onEscapeHit(TextField textField) {}
			public void onTabHit(TextField textField) {}
			public void onEnterHit(TextField textField) {
				final String commandLine;
				commandLine = textField.getText();

				if (println(commandLine, commandColor)) {
					inputBox.setText("");
					
					enteredCommands.push(commandLine);
					historylocation = -1;
					if (enteredCommands.size() > COMMANDHISTORY_MAXSIZE)
						enteredCommands.removeLast();
					
					onCommandEntered(commandLine);
				}
			}
		}); 

		inputBox.addKeyboardListener(new WidgetKeyboardAdapter() {
			@Override public void onKeyPressed(Widget widget, Key key, int modifierMask,
					long when) {
				switch (key.getKeyID()) {
				case UP:
					historylocation++; 
					if (historylocation >= enteredCommands.size()) historylocation = enteredCommands.size()-1;
					inputBox.setText(enteredCommands.get(historylocation));
					inputBox.setCaretPosition(Integer.MAX_VALUE);
					break;

				case DOWN:
					historylocation--; 
					if (historylocation < -1) historylocation = -1;
					if (historylocation >= 0)
						inputBox.setText(enteredCommands.get(historylocation));
					else
						inputBox.setText("");
					inputBox.setCaretPosition(Integer.MAX_VALUE);
					break;
				}
			}
		});
	}
	
	// private text field class used for the input box
	private static final class InputBox extends TextField {
		/**
		 * {@inheritDoc}
		 */
		@Override protected boolean blocksFocusMoveDeviceComponent(DeviceComponent dc) {
			if (super.blocksFocusMoveDeviceComponent(dc)) return true;

			if (dc == Keys.UP) return true;
			if (dc == Keys.DOWN) return true;

			return false;
		}

		public InputBox(float width, float height) {
			super(width, height);
		}

		public InputBox(float width, float height, TextField.Description desc) {
			super(width, height, "", desc);
		}
	}
	
	// Description Class - for describing what the final console should look like
	public static class Description extends BackgroundSettableWidget.BackgroundSettableDescriptionBase {
		private Border.Description borderDesc;
		private List.Description listDesc;
		private TextField.Description inputBoxDesc;
		private List.Description previewListDesc;
		private float listInputBoxGap;
		private float inputBoxHeight;
		private HUDFont listFont;
		private Colorf listFontColorNormal;
		private Colorf listFontColorWarning;
		private Colorf listFontColorError;
		private Colorf listFontColorCommand;

		public void setBorderDescription(Border.Description borderDesc) {this.borderDesc = borderDesc;}
		public Border.Description getBorderDescription() {return (borderDesc);}

		public void setListFontColorNormal(Colorf color) {this.listFontColorNormal = color;}
		public Colorf getListFontColorNormal() {return (listFontColorNormal);}

		public void setListFontColorWarning(Colorf color) {this.listFontColorWarning = color;}
		public Colorf getListFontColorWarning() {return (listFontColorWarning);}

		public void setListFontColorError(Colorf color) {this.listFontColorError = color;}
		public Colorf getListFontColorError() {return (listFontColorError);}
		
		public void setListFontColorCommand(Colorf color) {this.listFontColorCommand = color;}
		public Colorf getListFontColorCommand() {return listFontColorCommand;}

		public void setListFont(HUDFont listFont) {this.listFont = listFont;}
		public HUDFont getListFont() {return (listFont);}

		public void setListDescription(List.Description listDesc) {this.listDesc = listDesc;}
		public List.Description getListDescription() {return (listDesc);}

		public void setInputBoxDescription(TextField.Description inputBoxDesc) {this.inputBoxDesc = inputBoxDesc;}
		public TextField.Description getInputBoxDescription() {return (inputBoxDesc);}

		public void setPreviewListDescription(List.Description listDesc) {this.previewListDesc = listDesc;}
		public List.Description getPreviewListDescription() {return (previewListDesc);}

		public void setListInputBoxGap(float gap) {this.listInputBoxGap = gap;}
		public float getListInputBoxGap() {return (listInputBoxGap);}

		public void setInputBoxHeight(float height) {this.inputBoxHeight = height;}
		public float getInputBoxHeight() {return (inputBoxHeight);}

		@Override public Description clone() {return (new Description(this));}

		public Description(CheatConsole.Description template) {
			super(template.getBackgroundColor(), template.getBackgroundTexture());

			this.borderDesc = template.getBorderDescription().clone();
			this.listFontColorNormal = template.getListFontColorNormal();
			this.listFontColorWarning = template.getListFontColorWarning();
			this.listFontColorError = template.getListFontColorError();
			this.listFontColorCommand = template.getListFontColorCommand();
			this.listFont = template.getListFont();
			this.listDesc = template.getListDescription().clone();
			this.inputBoxDesc = template.getInputBoxDescription().clone();
			this.previewListDesc = template.getPreviewListDescription().clone();
			this.listInputBoxGap = template.getListInputBoxGap();
			this.inputBoxHeight = template.getInputBoxHeight();
		}

		public Description(Border.Description borderDesc, Texture2D backgroundTexture,
				Colorf backgroundColor, Colorf listFontColorNormal,
				Colorf listFontColorWarning, Colorf listFontColorError, Colorf listFontColorCommand,
				List.Description listDesc, TextField.Description inputBoxDesc,
				List.Description previewListDesc, float listTextFieldGap, float inputBoxHeight) {
			super(backgroundColor, backgroundTexture);

			this.borderDesc = borderDesc;
			this.listFontColorNormal = listFontColorNormal;
			this.listFontColorWarning = listFontColorWarning;
			this.listFontColorError = listFontColorError;
			this.listFontColorCommand = listFontColorCommand;
			this.listDesc = listDesc;
			this.inputBoxDesc = inputBoxDesc;
			this.previewListDesc = previewListDesc;
			this.listInputBoxGap = listTextFieldGap;
			this.inputBoxHeight = inputBoxHeight;
		}

		public Description() {
			super(Colorf.DARK_GRAY, null);

			this.borderDesc = new Border.Description(10, 10, 10, 10);
			this.listFontColorNormal = Colorf.WHITE;
			this.listFontColorWarning = Colorf.ORANGE;
			this.listFontColorError = Colorf.RED;
			this.listFontColorCommand = Colorf.GRAY40;
			this.listDesc = HUD.getTheme().getListDescription();
			this.inputBoxDesc = HUD.getTheme().getTextFieldDescription();
			this.listInputBoxGap = 10f;
			this.inputBoxHeight = 20f;

			listDesc.setBorderDescription(new Border.Description(2, 2, 2, 2, Colorf.LIGHT_GRAY));
			listDesc.setBackgroundColor(null);
			listDesc.setBackgroundTexture((Texture2D) null);

			inputBoxDesc.setBorderDescription(new Border.Description(2, 2, 2, 2, Colorf.LIGHT_GRAY));
			inputBoxDesc.setBackgroundColor(null);
			inputBoxDesc.setBackgroundTexture((Texture2D) null);
			inputBoxDesc.setFontColor(listFontColorNormal, false);
			inputBoxDesc.setCaretTexture("white_rect");

			this.previewListDesc = listDesc.clone();
			previewListDesc.setBackgroundColor(this.getBackgroundColor());
			previewListDesc.setBackgroundTexture(this.getBackgroundTexture());
		}

		public Description(Texture2D backgroundTexture) {
			this();

			this.setBackgroundColor(null);
			this.setBackgroundTexture(backgroundTexture);
		}

		public Description(Colorf backgroundColor) {
			this();

			this.setBackgroundColor(backgroundColor);
			this.setBackgroundTexture((Texture2D) null);
		}

		public Description(Texture2D backgroundTexture, Colorf listFontColorNormal,
				Colorf listFontColorWarning, Colorf listFontColorError) {
			this();

			this.setBackgroundColor(null);
			this.setBackgroundTexture(backgroundTexture);
			this.listFontColorNormal = listFontColorNormal;
			this.listFontColorWarning = listFontColorWarning;
			this.listFontColorError = listFontColorError;

			inputBoxDesc.setFontColor(listFontColorNormal, false);
		}

		public Description(Colorf backgroundColor, Colorf listFontColorNormal,
				Colorf listFontColorWarning, Colorf listFontColorError) {
			this();

			this.setBackgroundColor(backgroundColor);
			this.setBackgroundTexture((Texture2D) null);
			this.listFontColorNormal = listFontColorNormal;
			this.listFontColorWarning = listFontColorWarning;
			this.listFontColorError = listFontColorError;

			inputBoxDesc.setFontColor(listFontColorNormal, false);
		}
	}
}