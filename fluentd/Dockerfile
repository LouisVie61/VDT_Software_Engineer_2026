FROM fluent/fluentd:v1.19.0-debian-1.0

USER root
RUN gem install fluent-plugin-elasticsearch --no-document \
    && mkdir -p /fluentd/buffer /fluentd/pos /fluentd/incoming/jsonl /fluentd/incoming/csv \
    && chown -R fluent:fluent /fluentd
USER fluent

COPY --chown=fluent:fluent fluent.conf /fluentd/etc/fluent.conf
