use clap::Parser;
use dc_bundle_util::convert_dcd_to_textproto_str;
use std::fs::File;
use std::io::Write;
use std::path::PathBuf;

#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct Args {
    /// Input DCD file path
    #[arg(short, long)]
    input: PathBuf,

    /// Output file path for the text representation (optional, prints to stdout if not provided)
    #[arg(short, long)]
    output: Option<PathBuf>,
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args = Args::parse();

    match convert_dcd_to_textproto_str(&args.input) {
        Ok(text_representation) => {
            if let Some(output_path) = args.output {
                let mut output_file = File::create(&output_path)?;
                output_file.write_all(text_representation.as_bytes())?;
                println!(
                    "Successfully converted {} to {}",
                    args.input.display(),
                    output_path.display()
                );
            } else {
                println!("{}", text_representation);
            }
        }
        Err(e) => {
            eprintln!("Error converting file: {:?}", e);
            std::process::exit(1);
        }
    }

    Ok(())
}
