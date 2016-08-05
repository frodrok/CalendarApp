if (window.console) {
  console.log("app init, lets go");
}

function setupCalendar(calendarObject, events) {
  if (events.length < 1) {
    console.log("no events recieved");
  } else {
    console.log("setting up calendar with these events: ");
    events.forEach(console.log.bind(console));
  }

  $('#calendar').fullCalendar({
    header: {
      left: 'prev,next today',
      center: 'title',
      right: 'month,basicWeek,basicDay'
    },
    defaultDate: '2016-08-01',
    editable: false,
    eventLimit: true,
    events: events
  });
}

function setupNewCalendar(calendarObject, events) {
    if (events.length < 1) {
        console.log("no events recieved");
    } else {
        console.log("setting up calendar with these events: ");
        events.forEach(console.log.bind(console));
    }

    calendarObject.fullCalendar({
        header: {
            left: 'prev,next today',
            center: 'title',
            right: 'month,basicWeek,basicDay'
        },
        defaultDate: '2016-08-01',
        editable: false,
        eventLimit: true,
        events: events
    });
}