import { addTrailingSlash } from "../util";

export type RealmEndpoint = {
  id: string;
  labelKey: string;
  url: string;
};

export function buildRealmEndpoints(
  serverBaseUrl: string,
  realm: string,
): RealmEndpoint[] {
  const realmBase = `${addTrailingSlash(serverBaseUrl)}realms/${encodeURIComponent(realm)}`;
  return [
    {
      id: "issuer",
      labelKey: "openidIssuer",
      url: realmBase,
    },
    {
      id: "oidc",
      labelKey: "openidConfiguration",
      url: `${realmBase}/.well-known/openid-configuration`,
    },
    {
      id: "cas",
      labelKey: "casDiscoveryEndpoint",
      url: `${realmBase}/protocol/cas/.well-known/cas`,
    },
  ];
}
