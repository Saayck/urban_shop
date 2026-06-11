# Urban Shop Backend

## Local environment

The application reads credentials and signing keys from environment variables.
Use `.env.example` as the reference; Spring Boot does not load that file automatically.

Required variables:

```text
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
```

The JWT secret must be Base64 encoded and contain at least 32 random bytes.

To create the first global administrator, temporarily set:

```text
BOOTSTRAP_SUPER_ADMIN_ENABLED=true
BOOTSTRAP_SUPER_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_SUPER_ADMIN_PASSWORD=a-unique-password-with-12-or-more-characters
```

Start the application once, then set `BOOTSTRAP_SUPER_ADMIN_ENABLED=false`.
The password is never written to application logs.
