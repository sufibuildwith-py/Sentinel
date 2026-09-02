import { SentinelApiError } from "./api";

export type MutationContext = "reset" | "execute" | "approve" | "reject" | "generic";

export interface QueryErrorPresentation { label: string; message: string; requestId?: string; }

export function queryErrorPresentation(error: unknown): QueryErrorPresentation {
  if (!(error instanceof SentinelApiError)) {
    return error instanceof TypeError
      ? { label: "Backend unreachable", message: "Sentinel could not reach the configured API." }
      : { label: "Data unavailable", message: "The requested data could not be loaded." };
  }
  const requestId = error.requestId;
  if (error.status === 404) return { label: "Endpoint not found", message: "This console build requested an API route the backend does not expose.", requestId };
  if (error.status === 408 || error.status === 504) return { label: "Request timed out", message: "The backend did not answer within the allowed time.", requestId };
  if (error.status >= 500) return { label: "Server error", message: "The backend rejected this data request. Persisted state was not changed.", requestId };
  if (error.status === 422) return { label: "Schema or state mismatch", message: error.message || "The response could not be applied to the current state.", requestId };
  return { label: "Data unavailable", message: error.message || `Sentinel API returned ${error.status}.`, requestId };
}

export function mutationErrorMessage(error: unknown, context: MutationContext = "generic"): string {
  if (!(error instanceof SentinelApiError)) {
    return error instanceof TypeError
      ? "Sentinel could not reach the API. Check the backend connection and try again."
      : error instanceof Error ? error.message : "The operation could not be completed.";
  }

  const backend = error.message.trim();
  if (error.status === 400) return backend || "The request is invalid. Review the supplied values and try again.";
  if (error.status === 404) return context === "execute"
    ? "This recovery action no longer exists. The current incident state has been refreshed."
    : "The requested resource no longer exists. Refresh to load the current state.";
  if (error.status === 409) return context === "execute"
    ? "This recovery has already advanced. The current persisted state has been refreshed. No duplicate provider action was sent."
    : backend || "The persisted state changed before this operation completed. Refresh and try again.";
  if (error.status === 422) return backend || "This action is not eligible under the current policy and persisted state.";
  if (error.status === 429) return "The operation is temporarily limited by a safety or velocity rule. Wait before retrying.";
  if (error.status >= 500) return context === "reset"
    ? "Synthetic state was not reset because the backend could not complete the operation. Existing state is unchanged."
    : "The Sentinel backend is temporarily unavailable. Persisted state has not been changed by the console.";
  return backend || `Sentinel API returned ${error.status}.`;
}
