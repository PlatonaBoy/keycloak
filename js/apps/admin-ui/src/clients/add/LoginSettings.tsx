import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";

import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { convertAttributeNameToForm } from "../../util";
import { PROTOCOL_CAS } from "../constants";
import { FormFields } from "../ClientDetails";

type LoginSettingsProps = {
  protocol?: string;
};

export const LoginSettings = ({
  protocol = "openid-connect",
}: LoginSettingsProps) => {
  const { t } = useTranslation();
  const { watch } = useFormContext<FormFields>();

  const standardFlowEnabled = watch("standardFlowEnabled");
  const implicitFlowEnabled = watch("implicitFlowEnabled");
  const showServiceUrls =
    standardFlowEnabled || implicitFlowEnabled || protocol === PROTOCOL_CAS;

  return (
    <>
      <TextControl
        type="url"
        name="rootUrl"
        label={t("rootUrl")}
        labelIcon={t("rootURLHelp")}
      />
      <TextControl
        type="url"
        name="baseUrl"
        label={t("homeURL")}
        labelIcon={t("homeURLHelp")}
      />
      {showServiceUrls && (
        <>
          <FormGroup
            label={
              protocol === PROTOCOL_CAS
                ? t("casServiceUrls")
                : t("validRedirectUri")
            }
            fieldId="kc-redirect"
            labelIcon={
              <HelpItem
                helpText={
                  protocol === PROTOCOL_CAS
                    ? t("casServiceUrlsHelp")
                    : t("validRedirectURIsHelp")
                }
                fieldLabelId={
                  protocol === PROTOCOL_CAS
                    ? "casServiceUrls"
                    : "validRedirectUri"
                }
              />
            }
          >
            <MultiLineInput
              id="kc-redirect"
              name="redirectUris"
              aria-label={
                protocol === PROTOCOL_CAS
                  ? t("casServiceUrls")
                  : t("validRedirectUri")
              }
              addButtonLabel={
                protocol === PROTOCOL_CAS ? "addCasServiceUrl" : "addRedirectUri"
              }
            />
          </FormGroup>
          <FormGroup
            label={t("validPostLogoutRedirectUri")}
            fieldId="kc-postLogoutRedirect"
            labelIcon={
              <HelpItem
                helpText={t("validPostLogoutRedirectURIsHelp")}
                fieldLabelId="validPostLogoutRedirectUri"
              />
            }
          >
            <MultiLineInput
              id="kc-postLogoutRedirect"
              name={convertAttributeNameToForm(
                "attributes.post.logout.redirect.uris",
              )}
              aria-label={t("validPostLogoutRedirectUri")}
              addButtonLabel="addPostLogoutRedirectUri"
              stringify
            />
          </FormGroup>
        </>
      )}
      {protocol === "saml" && (
        <>
          <TextControl
            name="attributes.saml_idp_initiated_sso_url_name"
            label={t("idpInitiatedSsoUrlName")}
            labelIcon={t("idpInitiatedSsoUrlNameHelp")}
          />
          <TextControl
            name="attributes.saml_idp_initiated_sso_relay_state"
            label={t("idpInitiatedSsoRelayState")}
            labelIcon={t("idpInitiatedSsoRelayStateHelp")}
          />
          <TextControl
            type="url"
            name="adminUrl"
            label={t("masterSamlProcessingUrl")}
            labelIcon={t("masterSamlProcessingUrlHelp")}
          />
        </>
      )}
      {protocol !== "saml" && protocol !== PROTOCOL_CAS && standardFlowEnabled && (
        <FormGroup
          label={t("webOrigins")}
          fieldId="kc-web-origins"
          labelIcon={
            <HelpItem
              helpText={t("webOriginsHelp")}
              fieldLabelId="webOrigins"
            />
          }
        >
          <MultiLineInput
            id="kc-web-origins"
            name="webOrigins"
            aria-label={t("webOrigins")}
            addButtonLabel="addWebOrigins"
          />
        </FormGroup>
      )}
    </>
  );
};
