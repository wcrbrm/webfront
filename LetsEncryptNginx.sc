import ujson.Js
import ammonite.ops._
import ammonite.ops.ImplicitWd._

object LetsencryptNginx {
    val psName = "nginx-for-certbot"
    val version = "1.14.0"

    def getId: String = {
        try {
            val cmdResult: CommandResult = %%docker("inspect", psName)
            ujson.read(cmdResult.out.string)(0).obj("Id").str
        } catch {
            case _ => ""
        }
    }
    def running = {
        !(getId.isEmpty)
    }
    def stop = {
        %docker("stop", psName);
    }
    def start:Unit = {
        if (running) {
            println(psName + " already running")
        } else {
            println("starting " + psName)
            %docker("run", "-d",
                "--name", psName,
                "-p", "80:80",
                "-v", DockerVolume("letsencrypt-nginx-etc").asString + ":/etc/nginx",
                "-v", DockerVolume("letsencrypt-nginx-webroot").asString + ":/usr/share/nginx/html",
                "nginx:" + version)
        }
  }
}
