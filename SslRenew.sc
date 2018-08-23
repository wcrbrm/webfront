%docker("run", "--rm", "-it", "--name", "certbot",
  "-v", "/docker-volumes/etc/letsencrypt:/etc/letsencrypt",
  "-v", "/docker-volumes/var/lib/letsencrypt:/var/lib/letsencrypt",
  "-v", "/root/letsencrypt/letsencrypt-site:/data/letsencrypt",
  "-v", "/docker-volumes/var/log/letsencrypt:/var/log/letsencrypt",
  "certbot/certbot",
  "renew") 

//  "--quiet"