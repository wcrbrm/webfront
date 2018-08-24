import scala.collection.mutable.ListBuffer

/// --- --- --- --- --- --- --- --- --- ---
/// IP TABLES
/// --- --- --- --- --- --- --- --- --- ---

class IpTables {
  val lines:ListBuffer[String] = new ListBuffer[String]
  def asString = {
    "iptables -F\n" + lines.mkString("\n")
  }

  def filterTcpPorts(ips: List[String], ports: List[Int]): IpTables = {
    for (p <- ports) {
      lines += s"iptables -A INPUT -p tcp --dport $p -j DROP"
      lines += s"iptables -I INPUT 1 -p tcp --dport $p -s 127.0.0.1 -j ACCEPT"
      for (ip <- ips) {
        lines += s"iptables -I INPUT 1 -p tcp --dport $p -s $ip -j ACCEPT"
      }
    }
    this
  }
}
