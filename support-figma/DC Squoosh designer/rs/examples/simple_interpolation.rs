use dc_squoosh_parser::{KeyframeValue, PropertyLookup, Variant};
use serde_json::from_str;

fn main() {
    // 1. Sample Data representing the 'DesignCompose' plugin data stored on a Figma variant.
    // "Button-cornerRadius" has a custom keyframe at 0.5 (50%) with value [50, 50, 50, 50].
    // The Base64 string "WzUwLjAsIDUwLjAsIDUwLjAsIDUwLjBd" decodes to "[50.0, 50.0, 50.0, 50.0]".
    let json_data = r#"{
        "name": "Variant 1",
        "animation": {
            "spec": {
                "animation": { 
                    "Smooth": { 
                        "duration": { "secs": 1, "nanos": 0 }, 
                        "easing": "Linear" 
                    } 
                }
            },
            "customKeyframeData": {
                "Button-cornerRadius": "Linear|0.5,WzUwLjAsIDUwLjAsIDUwLjAsIDUwLjBd,Linear"
            }
        }
    }"#;

    // 2. Parse the Variant from JSON
    let variant: Variant = from_str(json_data).expect("Failed to parse Variant JSON");
    println!("Parsed Variant: {}", variant.name);

    // 3. Create PropertyLookup for efficient access
    let lookup = PropertyLookup::new(&variant);

    // 4. Retrieve the timeline for a specific node and property
    let node_name = "Button";
    let property = "cornerRadius";

    if let Some(timeline) = lookup.get(node_name, property) {
        println!("Found timeline for '{}' -> '{}'", node_name, property);

        // 5. Define Start and End values
        // In a real app, these come from the Scene Graph (Start Variant vs End Variant properties)
        let start_val = KeyframeValue::CornerRadii([0.0, 0.0, 0.0, 0.0]); // 0 radius
        let end_val = KeyframeValue::CornerRadii([100.0, 100.0, 100.0, 100.0]); // 100 radius

        println!("Interpolating from [0] to [100] with a keyframe of [50] at t=0.5");

        // 6. Interpolate at various time fractions
        for i in 0..=10 {
            let t = i as f32 / 10.0;
            let result = timeline.interpolate(&start_val, &end_val, t, None);

            match result {
                KeyframeValue::CornerRadii(radii) => {
                    println!(
                        "t={:.1}: [{:.1}, {:.1}, {:.1}, {:.1}]",
                        t, radii[0], radii[1], radii[2], radii[3]
                    );
                }
                _ => println!("t={:.1}: Unexpected value type", t),
            }
        }
    } else {
        println!("Timeline for {} not found!", property);
    }
}
