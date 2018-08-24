import ujson.Js
import ammonite.ops._
import ammonite.ops.ImplicitWd._

case class DockerVolume(val name: String) {
    def create = {
        %docker("volume", "create", name)
    }
    def inspect: String = {
        try {
            val cmdResult: CommandResult = %%docker("volume", "inspect", name)
            val mountPoint = ujson.read(cmdResult.out.string)(0).obj("Mountpoint").str
            mountPoint
        } catch {
            case _ => ""
        }
    }
    def remove = {
        %docker("volume", "rm", name)
    }
    def exists = {
        !inspect.isEmpty
    }
    def mountpoint: String = inspect
    def asString = {
       if (!exists) create
       name
    }
}


