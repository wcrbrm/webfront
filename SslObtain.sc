import ujson.Js
import ammonite.ops._
import ammonite.ops.ImplicitWd._

class SslObtainer(domain: String, email: String = "webcerebrium@gmail.com") {
    def handshake = {
        // WARNING: main NGINX should be turned off (80 port should be free)

        LetsencryptNginx.start
        val cmdReceiveCertificate = %docker("run", "-it", "--rm",
            "-v", DockerVolume("letsencrypt-etc").asString + ":/etc/letsencrypt",
            "-v", DockerVolume("letsencrypt-var-lib").asString + ":/var/lib/letsencrypt",
            "-v", DockerVolume("letsencrypt-var-log").asString + ":/var/log/letsencrypt",
            "-v", DockerVolume("letsencrypt-nginx-webroot").asString + ":/usr/share/nginx/html",
            "certbot/certbot",
            "certonly",
            "--webroot",
            "--email", email, "--agree-tos", "--no-eff-email",
            "--webroot-path=/usr/share/nginx/html",
            "-d", domain
        )

        // - Congratulations! Your certificate and chain have been saved at:
        // /etc/letsencrypt/live/{YOUR_DOMAIN}/fullchain.pem
        // - Your key file has been saved at:
        // /etc/letsencrypt/live/{YOUR_DOMAIN}/privkey.pem

        LetsencryptNginx.stop
        this
    }
    def copyKeys(destPath: Path = root / 'etc / 'letsencrypt / 'live) = {
        val pathPem: Path = Path(DockerVolume("letsencrypt-etc").mountpoint) / 'live / domain
        val files = List("cert.pem", "chain.pem", "fullchain.pem", "privkey.pem")
        val domainPath: Path = destPath / domain
        %mkdir("-p" , domainPath.toString)
        for ( f <- files ) {
            println("Copying from " + (pathPem / f) + " to " + (domainPath / f))
            %cp("-f", pathPem / f, domainPath / f)

        }
    }
}

// new SslObtainer( domain = "your-domain.com").handshake.copyKeys()
