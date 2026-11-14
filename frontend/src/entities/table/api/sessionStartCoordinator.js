export const createSessionStartCoordinator = () => {
  const inFlightRequests = new Map();

  return (tableCode, startSession) => {
    const existingRequest = inFlightRequests.get(tableCode);
    if (existingRequest) {
      return existingRequest;
    }

    let request;
    try {
      request = Promise.resolve(startSession(tableCode));
    } catch (error) {
      return Promise.reject(error);
    }

    inFlightRequests.set(tableCode, request);

    const clearRequest = () => {
      if (inFlightRequests.get(tableCode) === request) {
        inFlightRequests.delete(tableCode);
      }
    };

    request.then(clearRequest, clearRequest);
    return request;
  };
};

export const startTableSessionOnce = createSessionStartCoordinator();
