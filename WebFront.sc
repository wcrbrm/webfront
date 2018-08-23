/// --- --- --- --- --- --- --- --- --- ---
/// THIS IS A LIBRARY OF OBJECTS / CLASSES TO BE USED


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

/// --- --- --- --- --- --- --- --- --- ---
/// PATHS
/// --- --- --- --- --- --- --- --- --- ---

class LocationPath( path: String, prefix: String = "") {
  def asString: String = {
    if (!prefix.isEmpty)
      prefix + " " + path
    else
      path
  }
}

class RegexPath(path: String) extends LocationPath(path, prefix = "~")
object PhpPath extends LocationPath(path = ".php$", prefix = "~")
object RootPath extends LocationPath("/")
object WellKnownPath extends LocationPath(path = "/.well-known", prefix = "^")


/// --- --- --- --- --- --- --- --- --- ---
/// LOGGING
/// --- --- --- --- --- --- --- --- --- ---

class Logging( logging: String = "", alias: String = "") {
  def asString: String = {
    if (!logging.isEmpty && !alias.isEmpty)
      List(
        s"access_log /var/log/nginx/${alias}_access.log ${logging};\n",
        s"error_log /var/log/nginx/${alias}_error.log;\n"
      ).mkString
    else
      ""
  }
}

class CommonLogging(alias: String) extends Logging("main", alias)
object NoLogging extends Logging

/// --- --- --- --- --- --- --- --- --- ---
/// BACKENDS
/// --- --- --- --- --- --- --- --- --- ---

class ProxyBackend {
  val list = new ListBuffer[String]
  def pass(filepath: String): ProxyBackend = {
    list += "proxy_pass " + filepath + ";\n"
    this
  }
  def header(key: String, value: String): ProxyBackend = {
    list += "proxy_set_header " + key + " " + value + ";\n"
    this
  }
  def buffering(onOff: String): ProxyBackend = {
    list += "proxy_buffering " + onOff + ";\n"
    this
  }
  def redirect(onOff: String): ProxyBackend = {
    list += "proxy_redirect " + onOff + ";\n"
    this
  }
  def asList: List[String] = {
    list.toList
  }
}

class FastCGI {
  val list = new ListBuffer[String]
  def includeParams: FastCGI = {
    list += "include fastcgi_params;\n"
    this
  }
  def interceptErrors: FastCGI = {
    list += "fastcgi_intercept_errors on;\n"
    this
  }
  def pass(filepath: String): FastCGI = {
    list += "fastcgi_pass " + filepath + ";\n"
    this
  }
  def index(files: String): FastCGI = {
    list += "fastcgi_index " + files + ";\n"
    this
  }
  def param(key: String, value: String): FastCGI = {
    list += "fastcgi_param " + key + " " + value + ";\n"
    this
  }
  def script(value: String): FastCGI = param("SCRIPT_FILENAME", value)
  def defaultScript: FastCGI = script("$document_root$fastcgi_script_name")
  def asList: List[String] = {
    list.toList
  }
}

/// --- --- --- --- --- --- --- --- --- ---
/// CONFIG SECTIONS
/// --- --- --- --- --- --- --- --- --- ---

class NestedConfig {
  val level = 2
  def nested(inner: String): String = {
    val tabs = " " * level
    inner.trim.split("\n").map(line => tabs + line).mkString("\n")
  }
  def wrap(inner: String): String = {
    if (inner.split("\n").length <= 1)
      " { " + inner.trim() + " }\n"
    else
      " {\n" + nested( inner ) + "\n}\n"
  }
}

// trait Allow
// object AllowAll extends Allow {}

class Location( path: LocationPath, logging: Logging = NoLogging, root: String = "", // allow: Allow,
                children: List[String] = List()) extends NestedConfig {
  def asString: String = {
    val props: ListBuffer[String] = new ListBuffer()
    if (!root.isEmpty) props += s"root ${root};\n"
    val log: String = logging.asString
    if (!log.isEmpty) props += log
    "location " + this.path.asString + wrap(props.toList.mkString + children.mkString)
  }
}

class DeniedLocation(path: LocationPath) extends Location(path, children = List("deny all;"))
class DefaultLocation(root: String, logging: Logging = NoLogging)
  extends Location(RootPath, logging = logging, root = root, children = List("try_files $uri $uri/ =404;"))


class ServerRedirect(val host: String = "", val https: Boolean = false, val full: Boolean = false ) {
  def isEmpty: Boolean = host.isEmpty
  def asString: String = {
    val sb = new StringBuffer();
    val proto = if (https) "https" else "http"
    if (full) {
      sb.append("rewrite ^(.*)$ " + proto +"://" + host + "$1 permanent;")
    }
    // TODO: redirects that are not full
    sb.toString
  }
}

class ServerTimeout (
    val keepAlive:Int = -1,
    val clientHeader: Int =  -1,
    val clientBody: Int =  -1,
    val sendTimeout: Int = -1,
    val fastGgiRead: Int = -1
) {
  def isEmpty: Boolean = {
    keepAlive == -1 && clientHeader == -1 && clientBody == -1 && sendTimeout == -1 && fastGgiRead == -1
  }
  def asString: String = {
    val sb: StringBuffer = new StringBuffer();
    if (keepAlive >= 0) sb.append(s"keepalive_timeout = ${keepAlive};\n")
    if (clientHeader >= 0) sb.append(s"client_header_timeout = ${clientHeader};\n")
    if (clientBody >= 0) sb.append(s"client_body_timeout = ${clientBody};\n")
    if (sendTimeout >= 0) sb.append(s"send_timeout_timeout = ${sendTimeout};\n")
    if (fastGgiRead >= 0) sb.append(s"fastcgi_read_timeout = ${fastGgiRead};\n")
    sb.toString
  }
}

class ServerSslOptions(
    val pemFolder:String = "",
    val protocols:String = "TLSv1.2 TLSv1.1 TLSv1",
    val sts:Boolean = true,
    val preferServerCiphers: String = "on",
    val ciphers:String = "ECDH+AESGCM:ECDH+AES256:ECDH+AES128:DH+3DES:!ADH:!AECDH:!MD5",
    val ecdhCurve: String = "secp384r1",
    val sessionTickets: String = "off",
    val stapling: Boolean = false
    ) {
  def isEmpty: Boolean = pemFolder.isEmpty

  def asString: String = {
    val sb: StringBuffer = new StringBuffer();
    if (!isEmpty) {
      sb.append("ssl on;\n")
      sb.append(s"ssl_certificate ${pemFolder}/fullchain.pem;\n")
      sb.append(s"ssl_certificate_key ${pemFolder}/privkey.pem;\n")
      if (!protocols.isEmpty) sb.append(s"ssl_protocols ${protocols};\n")
      if (!preferServerCiphers.isEmpty) sb.append(s"ssl_prefer_server_ciphers ${preferServerCiphers};\n")
      if (!ciphers.isEmpty) sb.append(s"ssl_ciphers ${ciphers};\n")
      if (!ecdhCurve.isEmpty) sb.append(s"ssl_ecdh_curve ${ecdhCurve};\n")
      if (!sessionTickets.isEmpty) sb.append(s"ssl_session_tickets ${sessionTickets};\n")
      if (sts) {
        sb.append( "add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains; preload\";\n")
      }
    }
    sb.toString
  }
}


/// --- --- --- --- --- --- --- --- --- ---
/// SERVER SECTION
/// --- --- --- --- --- --- --- --- --- ---

class Server(
              port: Int,
              name: String,
              aliases: List[String] = List(),
              locations: List[Location] = List(),
              redirect: ServerRedirect = new ServerRedirect(),
              timeouts: ServerTimeout = new ServerTimeout(),
              ssl: ServerSslOptions = new ServerSslOptions()
) extends NestedConfig {

  val children: ListBuffer[String] = ListBuffer();
  if (port == 443) {
    children += "listen 443 ssl http2;\n"
    children += "listen [::]:443 ssl http2;\n"
  } else {
    children += "listen " + port + ";\n"
    children += "listen [::]:" + port + ";\n"
  }
  children += "server_name " + name + ";\n"

  var generated = false;
  def asString: String = {
    if (!generated) {
      for (alias <- aliases) {
        children += "server_name " + alias + ";\n"
      }
      if (locations.length > 0) {
        children += "charset utf-8" + ";\n"
      }
      if (!ssl.isEmpty) children += ssl.asString
      if (!timeouts.isEmpty) children += timeouts.asString
      for (l <- locations) {
        children += l.asString
      }
      if (!redirect.isEmpty) children += redirect.asString
      generated = true
    }
    "server" + wrap(children.mkString)
  }
}

class Upstream(name: String, servers: List[String] = List()) extends NestedConfig {

  // (round robin) - default
  // The default load balancing algorithm that is used if no other balancing directives are present.
  // Each server defined in the upstream context is passed requests sequentially in turn.
  var algorithm = ""

  def asString: String = {
    val children = new ListBuffer[String];
    if (!algorithm.isEmpty) {
      children += algorithm;
    }
    for (server <- servers) {
      children += "server " + server + ";\n"
    }
    "upstream " + name + " " + wrap(children.mkString);
  }
}



/// --- --- --- --- --- --- --- --- --- ---
/// MAIN SERVER CONFIG
/// --- --- --- --- --- --- --- --- --- ---

abstract class ServerConfig(val name: String) {
  val locations = new ListBuffer[Location];

  def getNginxConfigLocation: String = "/etc/nginx/sites-available/" + name + ".conf"
  def getNginxConfigEnabled: String = "/etc/nginx/sites-enabled/" + name + ".conf"

  def defaultHttpConfig(wwwToHost: Boolean = false): String = {
    val httpServer = new Server(80, name, locations = locations.toList);
    if (wwwToHost) {
      val redirect = new ServerRedirect(host = name, https = false, full = true);
      val WellKnownLocation = new Location(path = WellKnownPath, root = "/usr/local/share/html")
      val httpWwwServer = new Server(80, name, aliases = List("www." + name), redirect = redirect, locations = List(WellKnownLocation) );
      List(httpServer.asString, httpWwwServer.asString).mkString("\n")
    } else {
      httpServer.asString
    }
  }

  def defaultHttpsConfig(wwwToHost: Boolean = false): String = {
    val redirect = new ServerRedirect(host = name, https = true, full=true );
    val httpServer = new Server(80, name, aliases = List("www." + name), redirect = redirect);
    val ssl = new ServerSslOptions(pemFolder = s"/etc/nginx/letsencrypt/live/${name}", stapling = true)
    val httpsServer = new Server(443, name, locations = locations.toList, ssl = ssl);
    if (wwwToHost) {
      val httpsWwwServer = new Server(443, "www." + name, ssl = ssl, redirect = redirect);
      List(httpServer.asString, httpsWwwServer.asString, httpsServer.asString).mkString("\n")
    } else {
      List(httpServer.asString, httpsServer.asString).mkString("\n")
    }
  }
}

