import '@testing-library/jest-dom';

// Polyfill DataTransfer for jsdom environment.
// Unifies files[] and items.add into the same internal array.

class DataTransfer {
  constructor() {
    this._files = [];
  }
  get files() {
    return new Proxy(this._files, {
      get(target, prop) {
        const numProp = Number(prop);
        if (!Number.isNaN(numProp) && numProp >= 0 && numProp < target.length) {
          return target[numProp];
        }
        if (prop === 'length') {
          return target.length;
        }
        if (prop === 'item') {
          return (i) => (i >= 0 && i < target.length ? target[i] : null);
        }
        if (prop === Symbol.iterator) {
          return target[Symbol.iterator];
        }
        return target[prop];
      },
    });
  }
  get items() {
    const dt = this;
    return {
      add(file) {
        dt._files.push(file);
      },
    };
  }
}

global.DataTransfer = DataTransfer;
