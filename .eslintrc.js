module.exports = {
  extends: 'airbnb',
  rules: {
    semi: [2, 'never'],
  },
  parser: 'babel-eslint',
  parserOptions: {
    ecmaVersion: 7,
    ecmaFeatures: {
      experimentalObjectRestSpread: true,
    }
  }
}
