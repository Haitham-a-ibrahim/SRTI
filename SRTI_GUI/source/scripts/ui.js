// dealing with ui actions in general (except the canvas)

/* resizePanel()
	- Taken from online example, allows resizing horizonal panels to different widths.
*/
function resizePanel(element, direction, handler) {
    console.log("A resizePanel called");
    // Two variables for tracking positions of the cursor
    const drag = { x: 0, y: 0 };
    const delta = { x: 0, y: 0 };

    var first = document.getElementById("objectpanel");
    var second = document.getElementById("canvaspanel");
    let flag = true
    console.log("" + element.getAttribute("id"));
    if (element.getAttribute("id") == "separator2") {
        first = document.getElementById("canvaspanel");
        second = document.getElementById("inspectorpanel");
        flag = false
    }

	/* if present, the handler is where you move the DIV from
	   otherwise, move the DIV from anywhere inside the DIV */
    handler ? (handler.onmousedown = dragMouseDown) : (element.onmousedown = dragMouseDown);
    // function that will be called whenever the down event of the mouse is raised
    function dragMouseDown(e) {
        drag.x = e.clientX;
        drag.y = e.clientY;
        document.onmousemove = onMouseMove;
        document.onmouseup = () => { document.onmousemove = document.onmouseup = null; }
    }
    // function that will be called whenever the up event of the mouse is raised
    function onMouseMove(e) {
        const currentX = e.clientX;
        const currentY = e.clientY;

        delta.x = currentX - drag.x;
        delta.y = currentY - drag.y;

        const offsetLeft = element.offsetLeft;
        const offsetTop = element.offsetTop;


        let firstWidth = first.offsetWidth;
        let secondWidth = second.offsetWidth;
        if (direction === "H") // Horizontal
        {
            element.style.left = offsetLeft + delta.x + "px";
            firstWidth += delta.x;
            secondWidth -= delta.x;
        }
        drag.x = currentX;
        drag.y = currentY;
        first.style.width = firstWidth + "px";
        second.style.width = secondWidth + "px";
        if (flag) {
            $('#buttons-simulation').css('left', Math.max(firstWidth + 15, 176.86))
        }
    }

    console.log("A resizePanel ended");
    console.log("Name of second = " + second.getAttribute("id"));
}

/*	UpdateSelectedStateButtons()
	- Change image on each button based on what is selected.
*/
function UpdateSelectedStateButtons(selectId) {
    $('button[name="select-state-buttons"]').each(function (index) {
        if (index == selectId) {
            $(this).addClass('grey')
        } else {
            $(this).removeClass('grey')
        }
    })

    selectState = selectId;

    DisableCertainObjectButtons();
    ConfigureClearInspectorPanel();
}

/*	AddNewStage()
	- When '+' button is clicked, add a new stage.
*/
function AddNewStage() {
    numOfStages++;
    UpdateSelectedStage(stage);
}

/*	UpdateSelectedStage()
	- After clicking on one of the 'stage' numbers, 
		disable all simulator objects from previous stage and enable objects for new stage.
*/
function UpdateSelectedStage(btn_id) {
    stage = btn_id;

    // redraw buttons on top bar.
    let panel = $('#canvastabpanel')
    panel.empty()


    let i = 0, button
    for (i = 0; i < numOfStages; i++) {
        button = $('<a>').addClass('ui item').attr('name', i).text(i)
        button.click(function () {
            //TODO: check all this.attr
            // console.log(this)
            stage = parseInt(this.name)
            // console.log('stage' + stage)
            UpdateSelectedStage(stage)
        })

        if (stage == i) {
            button.addClass('active')
        } else {
            button.removeClass('active')
        }

        panel.append(button)
    }

    let menu = $('<div>').addClass('right menu')

    button = $('<a>').addClass('ui item').click(AddNewStage)
    button.append($('<i>').addClass('plus icon'))

    menu.append(button)
    panel.append(menu)

    CheckRedrawCanvasGrid();
    DrawAllArrowsOnCanvas();
    SetItemsVisibleInStage();
    DisableCertainObjectButtons();
}

/*	ClearObjectSubPanel1()
	- Clear sub-panel (canvas) on the main screen.
*/
function ClearObjectSubPanel1() {
    $('#objectsubpanel1').empty()
}

/*	AppendObjectToSubPanel1()
	- Append object to sub-panel (canvas) on the main screen.
*/
function AppendObjectToSubPanel1(simulator) {
    let panel = $('#objectsubpanel1')
    button = $('<button>').addClass('ui green button btn-list-item').text(simulator.name)
    button.data('id', simulator.name)
    button.data('ref', button)
    button.click(function () {
        elem = $(this)
        console.log("onclick at index = " + elem.data('id'));
        if (selectState == 0) {
            CreateNewSimulatorOnCanvas(elem.data('id'));
        } else if (selectState == 1) {
            ConfigureSimulatorFromList(elem.data('id'));
        } else if (selectState == 2) {

        } else if (selectState == 3) {
            DeleteSimulatorFromList(elem.data('id'), elem.data('ref'));
        }
    })
    panel.append(button)

}

/*	ResetObjectSubPanel1()
	- Reset sub-panel (canvas) on the main screen, that normally holds simulators.
*/
function ResetObjectSubPanel1() {
    let panel = $('#objectsubpanel1')
    panel.empty()
    let i = 0;
    for (let [name, simulator] of simulators) {
        AppendObjectToSubPanel1(simulator)
    }
}


/*	AppendObjectToSubPanel2()
	- Append object to sub-panel (canvas) on the main screen.
*/
function AppendObjectToSubPanel2(message) {
    let panel = $('#objectsubpanel2')
    button = $('<button>').addClass('ui blue button btn-list-item').text(message.name)
    button.data('id', message.name)
    button.data('ref', button)
    button.click(function () {
        elem = $(this)
        console.log("onclick at index = " + elem.data('id'));
        if (selectState == 0) {
            CreateNewMessageOnCanvas(elem.data('id'));
        } else if (selectState == 1) {
            ConfigureMessageFromList(elem.data('id'));
        } else if (selectState == 2) {

        } else if (selectState == 3) {
            DeleteMessageFromList(elem.data('id'), elem.data('ref'));
        }
    })
    panel.append(button)

}

/*	ResetObjectSubPanel2()
	- Reset sub-panel (canvas) on the main screen, that normally holds messages.
*/
function ResetObjectSubPanel2() {
    $('#objectsubpanel2').empty()
    let i = 0;
    for (let [name, message] of messages) {
        AppendObjectToSubPanel2(message)
    }
}

/*	DeleteSimulatorFromList()
	- Delete simulator from project, from list on the left and from the canvas.
*/
function DeleteSimulatorFromList(btn_id, child) {
    var deleteSimName = btn_id;

    console.log("Deleting sim from project, check if it's on the canvas = " + deleteSimName);
    let i = simulatorObjects.size - 1;
    for (let simObj of Array.from(simulatorObjects, values()).reverse()) {
        if (simObj.name == deleteSimName) {

            DeleteItemFromCanvasById(i, simObj);
        }
        i -= 1
    }

    simulators.delete(btn_id)
    child.remove()
    UpdateDrawArrowsAfterDelete(-1, -1);
}


/*	DeleteMessageFromList()
	- Delete message from project on left list and canvas in main screen.
*/
function DeleteMessageFromList(btn_id, child) {
    var deleteMessageName = btn_id;

    let msgObj = messageObjects.get(deleteMessageName)
    DeleteMessageFromCanvasById(msgObj);
    UpdateDrawArrowsAfterDelete(-1, btn_id);

    messages.delete(btn_id);
    child.remove()
}

/*	DisableCertainObjectButtons()
	- Disable certain buttons on the object panel, based on what simulators/messages are on the canvas.
*/
function DisableCertainObjectButtons() {
    var subpanel1btns = document.getElementById("objectsubpanel1").children;
    var subpanel2btns = document.getElementById("objectsubpanel2").children;

    let i = 0;
    let j = 0;
    for (j = 0; j < subpanel1btns.length; j++) {
        subpanel1btns[j].disabled = false;
    }
    for (j = 0; j < subpanel2btns.length; j++) {
        subpanel2btns[j].disabled = false;
    }
    // TODO: probably unnecessary loop
    //check simulator buttons to disable
    for (let simObj of simulatorObjects) {
        if (simObj.stage == stage && selectState == 0) {
            console.log("disable this button = " + simObj.name);
            for (j = 0; j < subpanel1btns.length; j++) {
                if (subpanel1btns[j].innerText == simObj.name) {
                    subpanel1btns[j].disabled = true;
                }
            }
        }
    }
    //check message buttons to disable
    if (selectState == 0) {

        for (j = 0; j < subpanel2btns.length; j++) {
            if (messageObjects.has(subpanel2btns[j].innerText)) {
                subpanel2btns[j].disabled = true;
            }
        }
    }

}

/*	ConfigureItemFromCanvas()
	- Click on item on canvas to configure (show options in inspector window on right).
*/
function ConfigureItemFromCanvas(e) {
    // include simulators, messages, AND the RTI Server itself
    // TODO: separate creation logic to different functions for reuse purposes
    // TODO: the logic for checking which object is clicked on should be O(1)

    ConfigureClearInspectorPanel();

    let panel = $('#inspectorpanel')
    let clickedOnItem = $(e.target).data('ref')
    let i = 0;

    if (simulatorObjects.has(clickedOnItem)) {
        let header = $('<div>').addClass('ui compact segment')
        let label = $('<label>').addClass('ui green large label').text(clickedOnItem.name)
        header.append($('<h3>').text('Simulator in Project'))
        header.append(label)

        let content = $('<div>').addClass('ui compact segment')
        let buttons = $('<div>').addClass('ui vertical buttons')

        let button0 = $('<button>').addClass('ui green basic button').text('Change Local Time')
        button0.click(() => {
            editExistingObject = clickedOnItem;
            EditSimLocalTime();
        })
        let button1 = $('<button>').addClass('ui green basic button').text('Set Initialize and Simulate Functions')
        button1.click(() => {
            editExistingObject = clickedOnItem;
            EditSimulateFunctions();
        })
        let button2 = $('<button>').addClass('ui green basic button').text('Change Stage Transition Conditions')
        button2.click(() => {
            editExistingObject = clickedOnItem;
            EditStageConditions();
        })
        let button3 = $('<button>').addClass('ui green basic button').text('Change End System Conditions')
        button3.click(() => {
            editExistingObject = clickedOnItem;
            EditEndConditions();
        })

        buttons.append(button0)
        buttons.append(button1)
        buttons.append(button2)
        buttons.append(button3)

        content.append(buttons)

        panel.append(header)
        panel.append(content)

        return;
    }

    if (typeof clickedOnItem !== 'undefined' && 'objectRef' in clickedOnItem && !('variables' in clickedOnItem)) {

        let messageName = clickedOnItem.name

        let panel = $('#inspectorpanel')
        let header = $('<div>').addClass('ui compact segment')
        let message = $('<label>').addClass('ui blue large label').text(messageName)
        header.append($('<h3>').text('Message on Canvas'))
        header.append(message)


        panel.append(header)

        return;
    }

    clickedOnItem = -1;
    var listOfSimPub = document.getElementsByClassName("div-canvas-pub");
    for (i = 0; i < listOfSimPub.length; i++) {
        if (e.target === listOfSimPub[i]) {
            clickedOnItem = i;
            break;
        }
    }
    if (clickedOnItem > -1) {
        ConfigureClearInspectorPanel();
        let simName = listOfSimPub[clickedOnItem].nameParent.name;
        let messageName = listOfSimPub[clickedOnItem].name;

        let panel = $('#inspectorpanel')
        let header = $('<div>').addClass('ui compact segment')
        let sim = $('<label>').addClass('ui green large label').text(simName)
        let arrow = $('<i>').addClass('arrow right icon')
        let message = $('<label>').addClass('ui blue large label').text(messageName)
        header.append($('<h3>').text('Publish-Definition on Canvas'))
        header.append(sim)
        header.append(arrow)
        header.append(message)

        let content = $('<div>').addClass('ui compact segment')
        let button = $('<button>').addClass('ui red basic button').text('Change Publish Parameters')
        button.click(() => {
            editExistingObject = listOfSimPub[clickedOnItem].nameParent;		//index in 'simulatorObjects' of sim
            editExistingObject2 = listOfSimPub[clickedOnItem].name;				//index in 'simulatorObjects[i].publishedMessages'
            EditPublishConnectionPrompt();
        })
        content.append(button)

        panel.append(header)
        panel.append(content)

        return;
    }

    clickedOnItem = -1;
    var listOfSimSub = document.getElementsByClassName("div-canvas-sub");
    for (i = 0; i < listOfSimSub.length; i++) {
        if (e.target === listOfSimSub[i]) {
            clickedOnItem = i;
            break;
        }
    }
    if (clickedOnItem > -1) {
        ConfigureClearInspectorPanel();
        let simName = listOfSimSub[clickedOnItem].nameParent.name;
        let messageName = listOfSimSub[clickedOnItem].name;
        let panel = $('#inspectorpanel')
        let header = $('<div>').addClass('ui compact segment')
        let message = $('<label>').addClass('ui blue large label').text(messageName)
        let arrow = $('<i>').addClass('arrow right icon')
        let sim = $('<label>').addClass('ui green large label').text(simName)
        header.append($('<h3>').text('Subscribe-Definition on Canvas'))
        header.append(message)
        header.append(arrow)
        header.append(sim)

        let content = $('<div>').addClass('ui compact segment')
        let button = $('<button>').addClass('ui red basic button').text('Change Subscribe Parameters')
        button.click(() => {
            editExistingObject = listOfSimSub[clickedOnItem].nameParent;
            editExistingObject2 = listOfSimSub[clickedOnItem].name;
            EditSubscribeConnectionPrompt();
        })
        content.append(button)

        panel.append(header)
        panel.append(content)

        return;
    }

    clickedOnItem = -1;
    var serverGUI = document.getElementsByClassName("div-canvas-server");
    if (e.target === serverGUI[0]) {
        clickedOnItem = 0;
    }
    if (clickedOnItem > -1) {
        let panel = $('#inspectorpanel')
        let header = $('<div>').addClass('ui compact segment')
        header.append($('<h3>').text('RTI Server'))

        let content = $('<div>').addClass('ui compact segment')
        let button = $('<button>').addClass('ui blue basic button').text('Change Launch Parameters')
        button.click(() => {
            EditServer();
        })
        content.append($('<p>').text("Host Name: " + hostName))
        content.append($('<p>').text("Port Number: " + portNumber))
        content.append(button)

        panel.append(header)
        panel.append(content)

        return;
    }
}

/*	ConfigureSimulatorFromList()
	- Show options in inspector (on right) to configure a selected simulator (in project, not canvas).
*/
function ConfigureSimulatorFromList(btn_id) {
    ConfigureClearInspectorPanel();

    let panel = $('#inspectorpanel')
    let selectedObject = simulators.get(btn_id)
    let header = $('<div>').addClass('ui compact segment')
    let label = $('<label>').addClass('ui green large label').text(selectedObject.name)
    header.append($('<h3>').text('Simulator in Project'))
    header.append(label)

    let content = $('<div>').addClass('ui compact segment')
    let warning = $('<div>').addClass('ui warning').text('WARNING: Editing properties of this Simulator after it has been added to the project canvas may cause unforeseen effects. Please check the properties of this Simulator on the canvas after any updates.')
    let divider = $('<div>').addClass('ui hidden divider')
    let button = $('<button>').addClass('ui green basic button').text('Change Properties')
    button.click(() => {
        editExistingObject = selectedObject;
        NewSimulatorObjectPrompt();
    })
    content.append(warning)
    content.append(divider)
    content.append(button)

    panel.append(header)
    panel.append(content)


}

/*	ConfigureMessageFromList()
	- Show options in inspector (on right) to configure a selected message (in project, not canvas).
*/
function ConfigureMessageFromList(btn_id) {
    ConfigureClearInspectorPanel();

    let panel = $('#inspectorpanel')
    let selectedObject = messages.get(btn_id)
    let header = $('<div>').addClass('ui compact segment')
    let label = $('<label>').addClass('ui blue large label').text(selectedObject.name)
    header.append($('<h3>').text('Message in Project'))
    header.append(label)

    let content = $('<div>').addClass('ui compact segment')
    let warning = $('<div>').addClass('ui warning').text('WARNING: Editing properties of this Message after it has been added to the project canvas may cause unforeseen effects. Please check the properties of this Message on the canvas after any updates.')
    let divider = $('<div>').addClass('ui hidden divider')
    let button = $('<button>').addClass('ui blue basic button').text('Change Properties')
    button.click(() => {
        editExistingObject = selectedObject;
        NewMessageObjectPrompt();
    })
    content.append(warning)
    content.append(divider)
    content.append(button)

    panel.append(header)
    panel.append(content)
}

/*	ConfigureClearInspectorPanel()
	- Clear items in inspector panel.
*/
function ConfigureClearInspectorPanel() {

    let inspectorPanel = $('#inspectorpanel')
    inspectorPanel.empty()
}