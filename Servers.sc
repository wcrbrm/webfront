
// production
case class Server(
    id: Option[String],
    realmId: String = "testgroup",
    groupId: String = "test",
    name: String,
    ip: String,
    tags: Option[List[String]] = None,
    comments: Option[String] = None,
    state: Option[String] = None,
    auth_method: String = "password",
    auth_user: String = "root",
    auth_password: Option[String],
    auth_privateKey: Option[String] = None
)

val test = Server(
    id = Some("test"),
    ip = "127.0.0.1:22",
    name = "test",
    comments = Some("Test server"),
    tags = Some(List( "test" ))
)


object ServersTerminalHelper {

    private val rootPath = System.getProperty("user.dir")
    val pemPath = (s:Server) => s"${rootPath}/${s.name}.exported.pem"

    def savePem(s: Server): Option[Server] = {
        if (s.auth_method == "pem" && s.auth_privateKey.isDefined) {
            import sys.process._
            val pw = new java.io.PrintWriter(new java.io.File(pemPath(s)))
            pw.write(s.auth_privateKey.get.replace("\\n", "\n"))
            pw.close

            println("PEM saved " + pemPath(s))
            (s"chmod 0600 ${pemPath(s)}"!)
        }    
        Some(s)
    }
    
    def alias(s: Server): Option[String] = {
        val sshOpts = "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no "
        if (s.auth_method == "password" && s.auth_password.isDefined) {
            Some(s"alias ssh-${s.name}='sshpass -p ${s.auth_password.get} ssh ${sshOpts} ${s.auth_user}@${s.ip}'")
        } else if (s.auth_method == "pem") {
            Some(s"alias ssh-${s.name}='ssh ${sshOpts} -i ${pemPath(s)} ${s.auth_user}@${s.ip}'")
        } else {
            println(s"skipped ${s.name}")
            None
        }
    }

    def saveAll(list: List[Server]) = {
        println(list.flatMap(savePem).flatMap(alias).mkString("\n"))
    }
}

val production = List(test)
ServersTerminalHelper.saveAll(production)
