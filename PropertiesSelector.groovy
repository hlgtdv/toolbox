class PropertiesSelector {

	private static final String DEFAULT_SELECTION = "*";
	private static final String NOT_FOUND = "no key prefix found";
	private static final String TOO_MANY_FOUND = "too many key prefixes found";

	public static Map<String, String> findAllProperties(Properties props, String selectedKeyEnd, String selectedValue) {
		Map<String, String> selection = props.entrySet().stream().filter(prop -> {
				String key = (String) prop.getKey();
				String value = (String) prop.getValue();

				return key.endsWith(selectedKeyEnd) && value.equals(selectedValue);
			}).collect(Collectors.toMap(prop -> (String) prop.getKey(), prop -> (String) prop.getValue()));

		return selection;
	}

	public static String findKeyPrefix(Properties props, String selectedKeyEnd, String selectedValue) {
		Map<String, String> selection = PropertiesSelector.findAllProperties(
			props, selectedKeyEnd, selectedValue);

		if (selection.isEmpty()) {
			selection = PropertiesSelector.findAllProperties(
				props, selectedKeyEnd, DEFAULT_SELECTION);

			if (selection.isEmpty()) {
				throw new RuntimeException(NOT_FOUND);
			}
		}

		if (selection.size() > 1) {
			throw new RuntimeException(TOO_MANY_FOUND);
		}
		String key = new ArrayList<>(selection.keySet()).get(0);

		return key.split(selectedKeyEnd)[0];
	}
}
