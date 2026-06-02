{{/*
Common labels, names, and rendering helpers.
*/}}

{{- define "banking.labels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: banking-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
banking/tier: {{ .tier | default "application" }}
{{- end -}}

{{- define "banking.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "banking.serviceAccountName" -}}
{{ .Release.Name }}-{{ .name }}-sa
{{- end -}}

{{- define "banking.image" -}}
{{ .Values.global.imageRegistry }}/{{ .service.image.name }}:{{ .service.image.tag | default .Values.global.imageTag }}
{{- end -}}

{{/*
Render env list. Substitutes Helm templates inside string values.
*/}}
{{- define "banking.envList" -}}
{{- range $k, $v := .env }}
- name: {{ $k }}
  value: {{ tpl (toString $v) $.ctx | quote }}
{{- end }}
{{- end -}}
