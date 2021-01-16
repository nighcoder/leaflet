# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

import ipyleaflet as lf
import argparse
import json
from operator import itemgetter

from traitlets import (CaselessStrEnum, Unicode, Tuple, List, Bool, CFloat,
                       Float, CInt, Int, Instance, Dict, Bytes, Any, Union, Undefined)

import ipywidgets as widgets
from ipywidgets import Color
from ipywidgets.widgets.trait_types import TypedTuple
from ipywidgets.widgets.widget_link import Link

HEADER = '''# Model State

This is a description of the model state for each widget in the core Jupyter
widgets library. The model ID of a widget is the id of the comm object the
widget is using. A reference to a widget is serialized to JSON as a string of
the form `"IPY_MODEL_<MODEL_ID>"`, where `<MODEL_ID>` is the model ID of a
previously created widget of the specified type.

This model specification is for ipywidgets 7.4.*, @jupyter-widgets/base 1.1.*,
and @jupyter-widgets/controls 1.4.*.

## Model attributes

Each widget in the Jupyter core widgets is represented below. The heading
represents the model name, module, and version, view name, module, and version
that the widget is registered with.

'''

NUMBER_MAP = {
    'int': 'number (integer)',
    'float': 'number (float)',
    'bool': 'boolean',
    'bytes': 'Bytes'
}

classes = {
"map": lf.Map, "map-style": lf.MapStyle, "search-control": lf.SearchControl, "legend-control": lf.LegendControl,
"attribution-control": lf.AttributionControl, "scale-control": lf.ScaleControl, "zoom-control": lf.ZoomControl,
"draw-control": lf.DrawControl, "split-map-control": lf.SplitMapControl, "measure-control": lf.MeasureControl,
"full-screen-control": lf.FullScreenControl, "widget-control": lf.WidgetControl, "geo-json": lf.GeoJSON,
"marker-cluster": lf.MarkerCluster, "circle": lf.Circle, "circle-marker": lf.CircleMarker,
"rectangle": lf.Rectangle, "polygon": lf.Polygon, "polyline": lf.Polyline, "ant-path": lf.AntPath,
"vector-tile-layer": lf.VectorTileLayer, "heatmap": lf.Heatmap, "video-overlay": lf.VideoOverlay,
"image-overlay": lf.ImageOverlay, "wms-layer": lf.WMSLayer, "local-tile-layer": lf.TileLayer,
"tile-layer": lf.TileLayer, "popup": lf.Popup, "marker": lf.Marker, "awesome-icon": lf.AwesomeIcon, "icon": lf.Icon, "magnifying-glass": lf.MagnifyingGlass, "div-icon": lf.DivIcon
}

def trait_type(trait):
    attributes = {}
    if isinstance(trait, CaselessStrEnum):
        w_type = 'string'
        attributes['enum'] = trait.values
    elif isinstance(trait, Unicode):
        w_type = 'string'
    elif isinstance(trait, (Tuple, List)):
        w_type = 'array'
    elif isinstance(trait, TypedTuple):
        w_type = 'array'
        attributes['items'] = trait_type(trait._trait)
    elif isinstance(trait, Bool):
        w_type = 'bool'
    elif isinstance(trait, (CFloat, Float)):
        w_type = 'float'
    elif isinstance(trait, (CInt, Int)):
        w_type = 'int'
    elif isinstance(trait, Color):
        w_type = 'color'
    elif isinstance(trait, Dict):
        w_type = 'object'
    elif isinstance(trait, Bytes):
        w_type = 'bytes'
    elif isinstance(trait, Instance) and issubclass(trait.klass, widgets.Widget):
        w_type = 'reference'
        attributes['widget'] = trait.klass.__name__
    elif isinstance(trait, Union):
        w_type = 'union'
        attributes['types'] = [trait_type(t) for t in trait.trait_types]
    elif isinstance(trait, Any):
        # In our case, these all happen to be values that are converted to
        # strings
        w_type = 'label'
    else:
        w_type = trait.__class__.__name__
    attributes['type'] = w_type
    if trait.allow_none:
        attributes['allow_none'] = True
    return attributes


def jsdefault(trait):
    if isinstance(trait, Instance):
        default = trait.make_dynamic_default()
        if issubclass(trait.klass, widgets.Widget):
            return 'reference to new instance'
    else:
        default = trait.default_value
        if isinstance(default, bytes) or isinstance(default, memoryview) or isinstance(default, type(Undefined)):
            default = trait.default_value_repr()
    return default

def jsonify(identifier, widget, widget_list):
    attributes = []
    for name, trait in widget.traits(sync=True).items():
        if name == '_view_count':
            # don't document this since it is totally experimental at this point
            continue

        if name == 'options':
            attribute = dict(
                name=name,
                help=trait.help or '',
                default=widget._default_options()
            )
        else:
            attribute = dict(
                name=name,
                help=trait.help or '',
                default=jsdefault(trait)
            )

        if identifier == 'attribution-control' and name == 'prefix':
            attribute["default"] = "leaflet-0.1.0"

        attribute.update(trait_type(trait))
        attributes.append(attribute)

    return dict(name=identifier, attributes=attributes)

def create_spec(widget_list):
    widget_data = []
    for widget_name, widget_cls in widget_list:
        if issubclass(widget_cls, Link):
            widget = widget_cls((widgets.IntSlider(), 'value'),
                                (widgets.IntSlider(), 'value'))
        elif issubclass(widget_cls, (widgets.SelectionRangeSlider,
                                     widgets.SelectionSlider)):
            widget = widget_cls(options=[1])
        elif issubclass (widget_cls, lf.LegendControl):
            widget = widget_cls({})
        elif issubclass (widget_cls, lf.SearchControl):
            widget = widget_cls(marker=lf.Marker())
        elif issubclass (widget_cls, lf.WidgetControl):
            widget = widget_cls(widget=widgets.DOMWidget())
        else:
            widget = widget_cls()

        widget_data.append(jsonify(widget_name, widget, widget_list))
    return widget_data

def format_output(data, pretty=False):
    if pretty:
        return json.dumps(data, sort_keys=True, indent=2, separators=(',', ': '))
    else:
        return json.dumps(data, sort_keys=True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Description of your program')
    parser.add_argument('-p', '--pretty', action='store_true',
        help='Pretty output?')
    parser.add_argument('-b', '--basemaps', action='store_true',
        help='Generate basemaps?')
    args = parser.parse_args()

    widgets_to_document = sorted([k, classes[k]] for k in classes)
    spec = create_spec(widgets_to_document)

    if args.basemaps:
        print(format_output(lf.basemaps, args.pretty))
    else:
        print(format_output(spec, args.pretty))
