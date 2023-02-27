db.runCommand(
    {
        createIndexes: "lystore_export",
        indexes: [
            {
                key: {
                    "status": 1,
                },
                name: "lystore_export_tags_index"
            }
        ]
    });