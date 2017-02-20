object Three {
  def doStuff() {
    println("Hello from three")
  }
}

object Main extends App {
  One.doStuff()
  Two.doStuff()
  Three.doStuff()
}
