package com.shruthan.sweet.desktop;

/** Starter map created for a new SWEET installation with no saved cities. */
final class SampleCity {

    static final String ID = "sweet-demo-v1";

    private SampleCity() {
    }

    static String json() {
        return """
                {
                  "id": "sweet-demo-v1",
                  "name": "Sweet Starter City",
                  "width": 20,
                  "height": 14,
                  "roads": [
                    {"id":"road-maple-west","name":"Maple Avenue","start":{"id":"intersection-west","name":"West Gate","x":2,"y":6},"end":{"id":"intersection-market","name":"Market Street","x":6,"y":6},"type":"AVENUE","direction":"HORIZONTAL","bidirectional":true,"length":4},
                    {"id":"road-maple-center","name":"Maple Avenue","start":{"id":"intersection-market","name":"Market Street","x":6,"y":6},"end":{"id":"intersection-center","name":"Central Square","x":10,"y":6},"type":"AVENUE","direction":"HORIZONTAL","bidirectional":true,"length":4},
                    {"id":"road-maple-east","name":"Maple Avenue","start":{"id":"intersection-center","name":"Central Square","x":10,"y":6},"end":{"id":"intersection-east","name":"East Gate","x":18,"y":6},"type":"AVENUE","direction":"HORIZONTAL","bidirectional":true,"length":8},
                    {"id":"road-pine-north","name":"Pine Street","start":{"id":"intersection-north","name":"North Park","x":10,"y":2},"end":{"id":"intersection-center","name":"Central Square","x":10,"y":6},"type":"STREET","direction":"VERTICAL","bidirectional":true,"length":4},
                    {"id":"road-pine-south","name":"Pine Street","start":{"id":"intersection-center","name":"Central Square","x":10,"y":6},"end":{"id":"intersection-south","name":"South Station","x":10,"y":10},"type":"STREET","direction":"VERTICAL","bidirectional":true,"length":4}
                  ],
                  "intersections": [
                    {"id":"intersection-west","name":"West Gate","x":2,"y":6,"trafficLight":null},
                    {"id":"intersection-market","name":"Market Street","x":6,"y":6,"trafficLight":null},
                    {"id":"intersection-center","name":"Central Square","x":10,"y":6,"trafficLight":{"id":"light-central","state":"RED","cycleDurationSeconds":20}},
                    {"id":"intersection-east","name":"East Gate","x":18,"y":6,"trafficLight":null},
                    {"id":"intersection-north","name":"North Park","x":10,"y":2,"trafficLight":null},
                    {"id":"intersection-south","name":"South Station","x":10,"y":10,"trafficLight":null}
                  ],
                  "vehicles": [
                    {"id":"vehicle-demo","name":"Demo Car","x":2,"y":6,"speedKph":30,"status":"IDLE","currentIntersection":{"id":"intersection-west","name":"West Gate","x":2,"y":6},"route":[],"destination":null}
                  ],
                  "buildings": [
                    {"id":"building-homes","name":"Maple Homes","x":2,"y":1,"width":3,"height":2,"type":"RESIDENTIAL"},
                    {"id":"building-market","name":"Corner Market","x":14,"y":2,"width":3,"height":2,"type":"COMMERCIAL"},
                    {"id":"building-station","name":"South Station","x":14,"y":10,"width":3,"height":2,"type":"PUBLIC"}
                  ]
                }
                """;
    }
}
