module.exports = {
    "transform": {
        ".(ts|tsx)": "<rootDir>/node_modules/ts-jest/preprocessor.js"
    },
    "testRegex": "(/__tests__/.*|\\.(test|spec))\\.(ts|tsx|js)$",
    "moduleFileExtensions": [
        "ts",
        "tsx",
        "js"
    ],
    "testPathIgnorePatterns": [
        "/node_modules/",
        "<rootDir>/lystore/build/",
        "<rootDir>/lystore/out/"
    ],
    "verbose": true,
    "testURL": "http://localhost/",
    "coverageDirectory": "coverage/front",
    moduleNameMapper: {
        '^axios$': require.resolve('axios'),
    },
    "coverageReporters": [
        "text",
        "cobertura"
    ]
};
