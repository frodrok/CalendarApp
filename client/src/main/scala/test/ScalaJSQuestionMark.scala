package test

import org.scalajs.dom
import org.scalajs.dom.html.Element
import org.scalajs.jquery.{JQueryEventObject, jQuery}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

object ScalaJSQuestionMark extends js.JSApp {

  @JSExport
  def addClickedMessage(): Unit = {
    appendPar(dom.document.body, "You clicked the button!")
  }

  def setupUI() = {
    val menu = jQuery("#menu")

    menu.find("ul li").css("display", "inline")

    menu.find("ul li").hover(in _, out _)

    menu.find("li").click((evt: JQueryEventObject) => {
      println(jQuery(evt.target).html())
    })




  }

  def in(evt: JQueryEventObject): scala.scalajs.js.Any = {
    jQuery(evt.target).css("text-decoration", "underline")
  }

  def out(evt: JQueryEventObject): scala.scalajs.js.Any = {
    jQuery(evt.target).css("text-decoration", "none")
  }

  def main(): Unit = {
    println("hasllo wurl scalajs")

    jQuery(dom.document).ready(() => {
      setupUI
    })

    // jQuery(appendPar(dom.document, "cyka blyat"))

    /* val parNode = dom.document.createElement("p")
    val textnode = dom.document.createTextNode("werlcome feridn")
    parNode.appendChild(textnode)
    dom.document.body.appendChild(parNode) */

  }

  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode = dom.document.createElement("p")
    val textNode = dom.document.createTextNode(text)
    parNode.appendChild(textNode)
    targetNode.appendChild(parNode)
  }

  /* $(document).ready(function() {

    var menu = $("#menu");

    menu.find("ul li").css("display", "inline");
    menu.find("ul li").hover(function() {
      $(this).css({"text-decoration": "underline", "cursor": "pointer"});
    }, function() {
      $(this).css("text-decoration", "none");
    });

    menu.find("li").click(function(e) {
      // console.log(e.target().innerHTML);
      console.log(e.target.innerHTML);
      $("#content").html("<div>hey now hey now" + e + "</div>");
      setupNewCalendar($("#calendar"), []);
    });

    menu.find("li:first").click();
  }) */

}
