# Free Domain Setup

Target free subdomain:

```text
javajobfit.is-a.dev
```

Important: `is-a.dev` is intended for developer/personal or non-commercial
software-related projects. This is suitable while JavaJobFit is a free MVP. If
the product becomes commercial later, move to a paid domain such as
`javajobfit.com`.

## is-a.dev Pull Request File

In the `is-a-dev/register` repository, create this file:

```text
domains/javajobfit.json
```

Use this JSON:

```json
{
  "owner": {
    "username": "abhishekpratapsingh2601-arch"
  },
  "records": {
    "CNAME": "abhishekpratapsingh2601-arch.github.io"
  }
}
```

## After The Pull Request Is Merged

In the `java-job-fit` GitHub repository:

1. Go to `Settings`.
2. Open `Pages`.
3. In `Custom domain`, enter:

```text
javajobfit.is-a.dev
```

4. Click `Save`.
5. Enable `Enforce HTTPS` once GitHub allows it.

Do not add a `CNAME` file before the `is-a.dev` pull request is merged.
