{{/*
Expand the name of the chart.
*/}}
{{- define "job-scanner.name" -}}
{{- .Chart.Name }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "job-scanner.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Image reference helper — supports optional registry prefix.
Usage: {{ include "job-scanner.image" (dict "registry" .Values.imageRegistry "name" "auth-service" "tag" .Values.imageTag) }}
*/}}
{{- define "job-scanner.image" -}}
{{- if .registry -}}
{{ .registry }}/{{ .name }}:{{ .tag }}
{{- else -}}
{{ .name }}:{{ .tag }}
{{- end -}}
{{- end }}
