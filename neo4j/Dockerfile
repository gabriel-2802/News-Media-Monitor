FROM neo4j:5.26

COPY entrypoint-wrapper.sh /entrypoint-wrapper.sh
RUN chmod +x /entrypoint-wrapper.sh

ENTRYPOINT ["tini", "-g", "--", "/entrypoint-wrapper.sh"]
CMD ["neo4j"]
