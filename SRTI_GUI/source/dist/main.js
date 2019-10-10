// Modules to control application life and create native browser window
const electron = require('electron');
const {app, BrowserWindow, Menu} = electron;
const path = require('path');

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow
//let splashWindow


const menutemplate = [
	{
		label: 'File',
		submenu: [
			{ label: 'New', role: 'new', 
				click: function(){
						/* To call external functions, in files loaded in index.html, this works!:
						
						mainWindow.webContents.executeJavaScript("alert('hello there!');");
						mainWindow.webContents.executeJavaScript("UpdateSelectedStateButtons(2)");
						*/
					mainWindow.webContents.executeJavaScript("NewProject()");	
				}
			},	
			{ label: 'Open', role: 'open',
				click: function(){
					mainWindow.webContents.executeJavaScript("OpenOpenProject()");	
				}
			},
			{ label: 'Save', role: 'save',
				click: function(){
					mainWindow.webContents.executeJavaScript("SaveProject()");	
				}
			},
			{ label: 'Save As', role: 'saveas',
				click: function(){
					mainWindow.webContents.executeJavaScript("OpenSaveAsProject()");	
				}
			},
			{
				label: 'Export Execute Files', role: 'exportexecute',
				click: function(){
					mainWindow.webContents.executeJavaScript("ExportExecuteFiles()");
					console.log("Clicked export");
				}
			},
			{ label: 'Close', role: 'close', 
				click: function(){
					app.quit();
				}
			}
		]
	},
	{
		label: 'Edit',
		submenu: [
			/*{ label: '(to add later)', role: '(to add later)'}*/
			{ label: 'Undo', accelerator: "Ctrl+Z",
				click: function(){
					mainWindow.webContents.executeJavaScript("Undo()");
					// to see console.log, run electron app with '--enable-logging' parameter
					console.log("Clicked Undo");
				}
			},
			{ label: 'Redo', accelerator: "Ctrl+Y",
				click: function(){
					mainWindow.webContents.executeJavaScript("Redo()");
				}
			}
		]
	},
	{
		label: 'View',
		submenu: [
			{ label: '(to add later)', role: '(to add later)'}
			/*{ label: 'Zoom In', role: 'zoom in' },
			{ label: 'Zoom Out', role: 'zoom out' }*/
		]
	}, 
	{
		label: 'Help',
		submenu: [
			{ label: 'About', role: 'about' },
			{ label: 'Documentation', role: 'documentation' }
		]
	}
]
/*const menutemplate = function() {
	{
		label: 'File',
		submenu: [
			{ label: 'New', role: 'new', 
				click: function(){
					
					
				},
			{ label: 'Open', role: 'open' },
			{ label: 'Save', role: 'save' },
			{ label: 'Save As', role: 'saveas'},
			{ label: 'Close', role: 'close', 
				click: function(){
					app.quit();
				}
			}
		]
	},
	{
		label: 'Edit',
		submenu: [
			{ label: 'Undo', role: 'undo' },
			{ label: 'Redo', role: 'redo' }
		]
	},
	{
		label: 'View',
		submenu: [
			{ label: 'Zoom In', role: 'zoom in' },
			{ label: 'Zoom Out', role: 'zoom out' }
		]
	}, 
	{
		label: 'Help',
		submenu: [
			{ label: 'About', role: 'about' },
			{ label: 'Documentation', role: 'documentation' }
		]
	}
}*/

const menu = Menu.buildFromTemplate(menutemplate)
//menu.getMenuItemById('undo').enabled = isEnabled;
Menu.setApplicationMenu(menu)


/*function createSplashScreen(){
	splashWindow = new BrowserWindow({
		width:400,
		height:400,
		frame:false
	})
	splashWindow.loadFile('indexsplash.html');
}*/


function createWindow () {
  // Create the browser window.
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 700,
    webPreferences: {
      nodeIntegration: true
    },
	minWidth: 500,
	minHeight: 600
  })

  // and load the index.html of the app.
  mainWindow.loadFile('index.html');
 
  
  /*mainWindow.webContents.on('did-finish-load', function(){
	mainWindow.webContents.executeJavaScript("alert('hello there!');");
	mainWindow.webContents.executeJavaScript("UpdateSelectedStateButtons(2)");
  });*/
  
  /*var mainJS;
  mainJS.loadURL(path.join(__dirname, 'index_main.js'));
  
  
  mainJS.webContents.send('call-foo', 'asadsa');//UpdateSelectedStateButtons(2);*/
  
  
  //UpdateSelectedStateButtons
  
  //UpdateSelectedStateButtons(2)
  //UpdateSelectedStateButtons(2);
  
  // Open the DevTools.
  // mainWindow.webContents.openDevTools()

  // Emitted when the window is closed.
  mainWindow.on('closed', function () {
    // Dereference the window object, usually you would store windows
    // in an array if your app supports multi windows, this is the time
    // when you should delete the corresponding element.
    mainWindow = null
  })
  
 
}

function onload(){
	
	 resizePanel( document.getElementById("seperator"), "H" );
	 //splashWindow.destroy();
}

function resizePanel( element, direction, handler )
		{
		  console.log("resizePanel called");
		  // Two variables for tracking positions of the cursor
		  const drag = { x : 0, y : 0 };
		  const delta = { x : 0, y : 0 };
		  /* if present, the handler is where you move the DIV from
			 otherwise, move the DIV from anywhere inside the DIV */
		  handler ? ( handler.onmousedown = dragMouseDown ): ( element.onmousedown = dragMouseDown );

		  // function that will be called whenever the down event of the mouse is raised
		  function dragMouseDown( e )
		  {
			drag.x = e.clientX;
			drag.y = e.clientY;
			document.onmousemove = onMouseMove;
			document.onmouseup = () => { document.onmousemove = document.onmouseup = null; }
		  }

		  // function that will be called whenever the up event of the mouse is raised
		  function onMouseMove( e )
		  {
			const currentX = e.clientX;
			const currentY = e.clientY;

			delta.x = currentX - drag.x;
			delta.y = currentY - drag.y;

			const offsetLeft = element.offsetLeft;
			const offsetTop = element.offsetTop;

			
			const first = document.getElementById("objectpanel");
			const second = document.getElementById("canvaspanel");
			let firstWidth = first.offsetWidth;
			let secondWidth = second.offsetWidth;
			if (direction === "H" ) // Horizontal
			{
				element.style.left = offsetLeft + delta.x + "px";
				firstWidth += delta.x;
				secondWidth -= delta.x;
			}
			drag.x = currentX;
			drag.y = currentY;
			first.style.width = firstWidth + "px";
			second.style.width = secondWidth + "px";
		  }
		  
		  console.log("resizePanel ended");
		}

function drag_start(event){
	
}

function drag_over(event){
	
}

function drop(event){
	
}

// function is used for dragging and moving



// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
//app.on('ready', createSplashScreen)
app.on('ready', createWindow)

// Quit when all windows are closed.
app.on('window-all-closed', function () {
  // On macOS it is common for applications and their menu bar
  // to stay active until the user quits explicitly with Cmd + Q
  if (process.platform !== 'darwin') app.quit()
})

app.on('activate', function () {
  // On macOS it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (mainWindow === null) createWindow()
})

app.on('onload', onload)


// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.
