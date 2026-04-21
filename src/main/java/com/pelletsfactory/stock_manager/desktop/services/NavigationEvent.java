package com.pelletsfactory.stock_manager.desktop.services;

import java.util.List;

public record NavigationEvent(
		String titulo,
		String subtitulo,
		List<String> breadcrumbs,
		ViewId viewId
) {}
